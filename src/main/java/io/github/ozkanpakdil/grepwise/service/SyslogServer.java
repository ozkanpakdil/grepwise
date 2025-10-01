package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server for receiving and processing syslog messages.
 * Supports both UDP and TCP protocols, and both RFC3164 and RFC5424 formats.
 */
@Service
public class SyslogServer {
    private static final Logger logger = LoggerFactory.getLogger(SyslogServer.class);
    // RFC3164 pattern: <PRI>Mmm dd hh:mm:ss HOSTNAME TAG: MSG
    private static final Pattern RFC3164_PATTERN =
            Pattern.compile("<(\\d+)>([A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+([^\\s]+)\\s+([^:]+):\\s+(.*)");
    // RFC5424 pattern: <PRI>VERSION TIMESTAMP HOSTNAME APP-NAME PROCID MSGID STRUCTURED-DATA MSG
    private static final Pattern RFC5424_PATTERN =
            Pattern.compile("<(\\d+)>(\\d+)\\s+(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+([^\\s]+)\\s+(?:\\[([^\\]]+)\\])?\\s*(.*)");
    private final LogBufferService logBufferService;
    private final Map<String, SyslogListener> activeListeners = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SyslogServer(LogBufferService logBufferService) {
        this.logBufferService = logBufferService;
        logger.info("SyslogServer initialized");
    }

    @PostConstruct
    public void init() {
        logger.info("SyslogServer started");
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down SyslogServer");
        stopAllListeners();
        executorService.shutdown();
    }

    /**
     * Start a syslog listener for the given configuration.
     *
     * @param config The syslog source configuration
     * @return true if the listener was started successfully, false otherwise
     */
    public boolean startListener(LogSourceConfig config) {
        if (config.getSourceType() != LogSourceConfig.SourceType.SYSLOG) {
            logger.error("Cannot start syslog listener for non-syslog source type: {}", config.getSourceType());
            return false;
        }

        String listenerId = config.getId();
        if (activeListeners.containsKey(listenerId)) {
            logger.warn("Syslog listener already running for config: {}", listenerId);
            return true;
        }

        try {
            SyslogListener listener;
            if ("UDP".equalsIgnoreCase(config.getSyslogProtocol())) {
                listener = new UdpSyslogListener(config);
            } else if ("TCP".equalsIgnoreCase(config.getSyslogProtocol())) {
                listener = new TcpSyslogListener(config);
            } else {
                logger.error("Unsupported syslog protocol: {}", config.getSyslogProtocol());
                return false;
            }

            activeListeners.put(listenerId, listener);
            executorService.submit(listener);
            logger.info("Started {} syslog listener on port {} for config: {}",
                    config.getSyslogProtocol(), config.getSyslogPort(), listenerId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to start syslog listener for config: {}", listenerId, e);
            return false;
        }
    }

    /**
     * Stop a syslog listener for the given configuration.
     *
     * @param configId The ID of the syslog source configuration
     * @return true if the listener was stopped successfully, false otherwise
     */
    public boolean stopListener(String configId) {
        SyslogListener listener = activeListeners.remove(configId);
        if (listener != null) {
            listener.stop();
            logger.info("Stopped syslog listener for config: {}", configId);
            return true;
        } else {
            logger.warn("No active syslog listener found for config: {}", configId);
            return false;
        }
    }

    /**
     * Stop all active syslog listeners.
     */
    public void stopAllListeners() {
        logger.info("Stopping all syslog listeners");
        for (Map.Entry<String, SyslogListener> entry : activeListeners.entrySet()) {
            entry.getValue().stop();
            logger.info("Stopped syslog listener for config: {}", entry.getKey());
        }
        activeListeners.clear();
    }

    /**
     * Get the number of active syslog listeners.
     *
     * @return The number of active listeners
     */
    public int getActiveListenerCount() {
        return activeListeners.size();
    }

    /**
     * Parse a syslog message and create a LogEntry.
     *
     * @param message The syslog message to parse
     * @param format  The format of the message (RFC3164 or RFC5424)
     * @param source  The source of the message (e.g., "syslog-udp:1514")
     * @return A LogEntry representing the syslog message
     */
    private LogEntry parseSyslogMessage(String message, String format, String source) {
        try {
            if ("RFC3164".equalsIgnoreCase(format)) {
                return parseRfc3164Message(message, source);
            } else if ("RFC5424".equalsIgnoreCase(format)) {
                return parseRfc5424Message(message, source);
            } else {
                logger.warn("Unsupported syslog format: {}. Treating as raw message.", format);
                return createRawLogEntry(message, source);
            }
        } catch (Exception e) {
            logger.error("Error parsing syslog message: {}", message, e);
            return createRawLogEntry(message, source);
        }
    }

    /**
     * Parse an RFC3164 (BSD syslog) message.
     *
     * @param message The message to parse
     * @param source  The source of the message
     * @return A LogEntry representing the message
     */
    private LogEntry parseRfc3164Message(String message, String source) {
        Matcher matcher = RFC3164_PATTERN.matcher(message);
        if (matcher.matches()) {
            int priority = Integer.parseInt(matcher.group(1));
            String timestamp = matcher.group(2);
            String hostname = matcher.group(3);
            String tag = matcher.group(4);
            String msg = matcher.group(5);

            // Extract facility and severity from priority
            int facility = priority / 8;
            int severity = priority % 8;

            // Parse timestamp
            LocalDateTime dateTime = parseRfc3164Timestamp(timestamp);
            long recordTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // Determine log level from severity
            String level = getSeverityLevel(severity);

            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("facility", String.valueOf(facility));
            metadata.put("severity", String.valueOf(severity));
            metadata.put("hostname", hostname);
            metadata.put("tag", tag);
            metadata.put("protocol", source.startsWith("syslog-udp") ? "UDP" : "TCP");

            return new LogEntry(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    recordTime,
                    level,
                    msg,
                    source,
                    metadata,
                    message
            );
        } else {
            logger.warn("Message does not match RFC3164 format: {}", message);
            return createRawLogEntry(message, source);
        }
    }

    /**
     * Parse an RFC5424 (structured syslog) message.
     *
     * @param message The message to parse
     * @param source  The source of the message
     * @return A LogEntry representing the message
     */
    private LogEntry parseRfc5424Message(String message, String source) {
        Matcher matcher = RFC5424_PATTERN.matcher(message);
        if (matcher.matches()) {
            int priority = Integer.parseInt(matcher.group(1));
            // String version = matcher.group(2);
            String timestamp = matcher.group(3);
            String hostname = matcher.group(4);
            String appName = matcher.group(5);
            String procId = matcher.group(6);
            String msgId = matcher.group(7);
            String structuredData = matcher.group(8);
            String msg = matcher.group(9);

            // Extract facility and severity from priority
            int facility = priority / 8;
            int severity = priority % 8;

            // Parse timestamp
            long recordTime = parseRfc5424Timestamp(timestamp);

            // Determine log level from severity
            String level = getSeverityLevel(severity);

            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("facility", String.valueOf(facility));
            metadata.put("severity", String.valueOf(severity));
            metadata.put("hostname", hostname);
            metadata.put("app_name", appName);
            metadata.put("proc_id", procId);
            metadata.put("msg_id", msgId);
            metadata.put("structured_data", structuredData);
            metadata.put("protocol", source.startsWith("syslog-udp") ? "UDP" : "TCP");

            return new LogEntry(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(),
                    recordTime,
                    level,
                    msg,
                    source,
                    metadata,
                    message
            );
        } else {
            logger.warn("Message does not match RFC5424 format: {}", message);
            return createRawLogEntry(message, source);
        }
    }

    /**
     * Create a raw log entry for a message that couldn't be parsed.
     *
     * @param message The raw message
     * @param source  The source of the message
     * @return A LogEntry representing the raw message
     */
    private LogEntry createRawLogEntry(String message, String source) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("protocol", source.startsWith("syslog-udp") ? "UDP" : "TCP");

        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "UNKNOWN",
                message,
                source,
                metadata,
                message
        );
    }

    /**
     * Parse an RFC3164 timestamp (e.g., "Jan 23 14:59:01").
     *
     * @param timestamp The timestamp string
     * @return A LocalDateTime representing the timestamp
     */
    private LocalDateTime parseRfc3164Timestamp(String timestamp) {
        // RFC3164 timestamps don't include the year, so we need to add it
        int currentYear = LocalDateTime.now().getYear();
        String timestampWithYear = timestamp + " " + currentYear;

        // Parse the timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss yyyy");
        return LocalDateTime.parse(timestampWithYear, formatter);
    }

    /**
     * Parse an RFC5424 timestamp (e.g., "2023-01-23T14:59:01.123Z").
     *
     * @param timestamp The timestamp string
     * @return The Unix timestamp (milliseconds since epoch)
     */
    private long parseRfc5424Timestamp(String timestamp) {
        // RFC5424 timestamps are ISO 8601 format
        return DateTimeRegexPatterns.extractDateTimeToTimestamp(timestamp);
    }

    /**
     * Get the log level corresponding to a syslog severity.
     *
     * @param severity The syslog severity (0-7)
     * @return The corresponding log level
     */
    private String getSeverityLevel(int severity) {
        return switch (severity) {
            case 0 -> "EMERGENCY"; // Emergency: system is unusable
            case 1 -> "ALERT";     // Alert: action must be taken immediately
            case 2 -> "CRITICAL";  // Critical: critical conditions
            case 3 -> "ERROR";     // Error: error conditions
            case 4 -> "WARN";      // Warning: warning conditions
            case 5 -> "NOTICE";    // Notice: normal but significant condition
            case 6 -> "INFO";      // Informational: informational messages
            case 7 -> "DEBUG";     // Debug: debug-level messages
            default -> "UNKNOWN";
        };
    }

    /**
     * Abstract base class for syslog listeners.
     */
    private abstract class SyslogListener implements Runnable {
        protected final LogSourceConfig config;
        protected final AtomicBoolean running = new AtomicBoolean(true);

        public SyslogListener(LogSourceConfig config) {
            this.config = config;
        }

        public void stop() {
            running.set(false);
        }

        protected void processMessage(String message, String source) {
            LogEntry logEntry = parseSyslogMessage(message, config.getSyslogFormat(), source);
            logBufferService.addToBuffer(logEntry);
        }
    }

    /**
     * UDP syslog listener.
     */
    private class UdpSyslogListener extends SyslogListener {
        private final DatagramSocket socket;

        public UdpSyslogListener(LogSourceConfig config) throws SocketException {
            super(config);
            this.socket = new DatagramSocket(config.getSyslogPort());
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            logger.info("UDP syslog listener started on port {}", config.getSyslogPort());

            while (running.get()) {
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    String source = "syslog-udp:" + config.getSyslogPort();

                    logger.trace("Received UDP syslog message: {}", message);
                    processMessage(message, source);

                    // Reset the packet for the next receive
                    packet.setLength(buffer.length);
                } catch (IOException e) {
                    if (running.get()) {
                        logger.error("Error receiving UDP syslog message", e);
                    }
                }
            }

            socket.close();
            logger.info("UDP syslog listener stopped on port {}", config.getSyslogPort());
        }

        @Override
        public void stop() {
            super.stop();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * TCP syslog listener.
     */
    private class TcpSyslogListener extends SyslogListener {
        private final ServerSocket serverSocket;

        public TcpSyslogListener(LogSourceConfig config) throws IOException {
            super(config);
            this.serverSocket = new ServerSocket();
            this.serverSocket.bind(new InetSocketAddress(config.getSyslogPort()));
        }

        @Override
        public void run() {
            logger.info("TCP syslog listener started on port {}", config.getSyslogPort());

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(new TcpClientHandler(clientSocket, config));
                } catch (IOException e) {
                    if (running.get()) {
                        logger.error("Error accepting TCP syslog connection", e);
                    }
                }
            }

            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing TCP server socket", e);
            }

            logger.info("TCP syslog listener stopped on port {}", config.getSyslogPort());
        }

        @Override
        public void stop() {
            super.stop();
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.error("Error closing TCP server socket", e);
                }
            }
        }
    }

    /**
     * Handler for TCP syslog client connections.
     */
    private class TcpClientHandler implements Runnable {
        private final Socket clientSocket;
        private final LogSourceConfig config;

        public TcpClientHandler(Socket clientSocket, LogSourceConfig config) {
            this.clientSocket = clientSocket;
            this.config = config;
        }

        @Override
        public void run() {
            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            logger.trace("TCP syslog client connected: {}", clientAddress);

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(clientSocket.getInputStream()))) {

                String source = "syslog-tcp:" + config.getSyslogPort();
                String line;

                while ((line = reader.readLine()) != null) {
                    logger.trace("Received TCP syslog message: {}", line);
                    // Create a log entry from the message and add it to the buffer
                    LogEntry logEntry = parseSyslogMessage(line, config.getSyslogFormat(), source);
                    logBufferService.addToBuffer(logEntry);
                }
            } catch (IOException e) {
                logger.error("Error handling TCP syslog client", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    logger.error("Error closing TCP client socket", e);
                }

                logger.trace("TCP syslog client disconnected: {}", clientAddress);
            }
        }
    }
}