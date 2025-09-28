package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Configuration for log sources including file directories, syslog, and HTTP.
 * This extends the concept of LogDirectoryConfig to support multiple source types.
 */
public class LogSourceConfig {
    private String id;
    private String name;
    private boolean enabled;
    private SourceType sourceType;

    // File source specific fields
    private String directoryPath;
    private String filePattern;
    private long scanIntervalSeconds;

    // Syslog source specific fields
    private int syslogPort;
    private String syslogProtocol; // UDP or TCP
    private String syslogFormat; // RFC3164 or RFC5424

    // HTTP source specific fields
    private String httpEndpoint;
    private String httpAuthToken;
    private boolean requireAuth;

    // CloudWatch source specific fields
    private String awsRegion;
    private String awsAccessKey;
    private String awsSecretKey;
    private String logGroupName;
    private String logStreamName;
    private long queryRefreshIntervalSeconds;

    /**
     * Default constructor with default values.
     */
    public LogSourceConfig() {
        this.enabled = true;
        this.sourceType = SourceType.FILE;
        this.filePattern = "*.log";
        this.scanIntervalSeconds = 60;
        this.syslogPort = 1514;
        this.syslogProtocol = "UDP";
        this.syslogFormat = "RFC5424";
        this.httpEndpoint = "/api/logs";
        this.requireAuth = true;
        this.awsRegion = "us-east-1";
        this.logGroupName = "";
        this.logStreamName = "";
        this.queryRefreshIntervalSeconds = 60;
    }

    /**
     * Constructor for file source type.
     */
    public static LogSourceConfig createFileSource(String id, String name, String directoryPath,
                                                   String filePattern, long scanIntervalSeconds, boolean enabled) {
        LogSourceConfig config = new LogSourceConfig();
        config.id = id;
        config.name = name;
        config.sourceType = SourceType.FILE;
        config.directoryPath = directoryPath;
        config.filePattern = filePattern;
        config.scanIntervalSeconds = scanIntervalSeconds;
        config.enabled = enabled;
        return config;
    }

    /**
     * Constructor for syslog source type.
     */
    public static LogSourceConfig createSyslogSource(String id, String name, int syslogPort,
                                                     String syslogProtocol, String syslogFormat, boolean enabled) {
        LogSourceConfig config = new LogSourceConfig();
        config.id = id;
        config.name = name;
        config.sourceType = SourceType.SYSLOG;
        config.syslogPort = syslogPort;
        config.syslogProtocol = syslogProtocol;
        config.syslogFormat = syslogFormat;
        config.enabled = enabled;
        return config;
    }

    /**
     * Constructor for HTTP source type.
     */
    public static LogSourceConfig createHttpSource(String id, String name, String httpEndpoint,
                                                   String httpAuthToken, boolean requireAuth, boolean enabled) {
        LogSourceConfig config = new LogSourceConfig();
        config.id = id;
        config.name = name;
        config.sourceType = SourceType.HTTP;
        config.httpEndpoint = httpEndpoint;
        config.httpAuthToken = httpAuthToken;
        config.requireAuth = requireAuth;
        config.enabled = enabled;
        return config;
    }

    /**
     * Constructor for CloudWatch source type.
     */
    public static LogSourceConfig createCloudWatchSource(String id, String name, String awsRegion,
                                                         String logGroupName, String logStreamName,
                                                         String awsAccessKey, String awsSecretKey,
                                                         long queryRefreshIntervalSeconds, boolean enabled) {
        LogSourceConfig config = new LogSourceConfig();
        config.id = id;
        config.name = name;
        config.sourceType = SourceType.CLOUDWATCH;
        config.awsRegion = awsRegion;
        config.logGroupName = logGroupName;
        config.logStreamName = logStreamName;
        config.awsAccessKey = awsAccessKey;
        config.awsSecretKey = awsSecretKey;
        config.queryRefreshIntervalSeconds = queryRefreshIntervalSeconds;
        config.enabled = enabled;
        return config;
    }

    /**
     * Convert from legacy LogDirectoryConfig.
     */
    public static LogSourceConfig fromLogDirectoryConfig(LogDirectoryConfig legacyConfig) {
        return createFileSource(
                legacyConfig.getId(),
                "Converted from " + legacyConfig.getDirectoryPath(),
                legacyConfig.getDirectoryPath(),
                legacyConfig.getFilePattern(),
                legacyConfig.getScanIntervalSeconds(),
                true
        );
    }

    /**
     * Convert to legacy LogDirectoryConfig (for backward compatibility).
     */
    public LogDirectoryConfig toLogDirectoryConfig() {
        if (sourceType != SourceType.FILE) {
            throw new IllegalStateException("Only FILE source type can be converted to LogDirectoryConfig");
        }

        LogDirectoryConfig legacyConfig = new LogDirectoryConfig();
        legacyConfig.setId(id);
        legacyConfig.setDirectoryPath(directoryPath);
        legacyConfig.setFilePattern(filePattern);
        legacyConfig.setScanIntervalSeconds(scanIntervalSeconds);
        legacyConfig.setEnabled(enabled);
        return legacyConfig;
    }

    public String getId() {
        return id;
    }

    // Getters and setters

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public long getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(long scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public int getSyslogPort() {
        return syslogPort;
    }

    public void setSyslogPort(int syslogPort) {
        this.syslogPort = syslogPort;
    }

    public String getSyslogProtocol() {
        return syslogProtocol;
    }

    public void setSyslogProtocol(String syslogProtocol) {
        this.syslogProtocol = syslogProtocol;
    }

    public String getSyslogFormat() {
        return syslogFormat;
    }

    public void setSyslogFormat(String syslogFormat) {
        this.syslogFormat = syslogFormat;
    }

    public String getHttpEndpoint() {
        return httpEndpoint;
    }

    public void setHttpEndpoint(String httpEndpoint) {
        this.httpEndpoint = httpEndpoint;
    }

    public String getHttpAuthToken() {
        return httpAuthToken;
    }

    public void setHttpAuthToken(String httpAuthToken) {
        this.httpAuthToken = httpAuthToken;
    }

    public boolean isRequireAuth() {
        return requireAuth;
    }

    public void setRequireAuth(boolean requireAuth) {
        this.requireAuth = requireAuth;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getLogGroupName() {
        return logGroupName;
    }

    public void setLogGroupName(String logGroupName) {
        this.logGroupName = logGroupName;
    }

    public String getLogStreamName() {
        return logStreamName;
    }

    public void setLogStreamName(String logStreamName) {
        this.logStreamName = logStreamName;
    }

    public long getQueryRefreshIntervalSeconds() {
        return queryRefreshIntervalSeconds;
    }

    public void setQueryRefreshIntervalSeconds(long queryRefreshIntervalSeconds) {
        this.queryRefreshIntervalSeconds = queryRefreshIntervalSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogSourceConfig that = (LogSourceConfig) o;
        return enabled == that.enabled &&
                scanIntervalSeconds == that.scanIntervalSeconds &&
                syslogPort == that.syslogPort &&
                requireAuth == that.requireAuth &&
                queryRefreshIntervalSeconds == that.queryRefreshIntervalSeconds &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                sourceType == that.sourceType &&
                Objects.equals(directoryPath, that.directoryPath) &&
                Objects.equals(filePattern, that.filePattern) &&
                Objects.equals(syslogProtocol, that.syslogProtocol) &&
                Objects.equals(syslogFormat, that.syslogFormat) &&
                Objects.equals(httpEndpoint, that.httpEndpoint) &&
                Objects.equals(httpAuthToken, that.httpAuthToken) &&
                Objects.equals(awsRegion, that.awsRegion) &&
                Objects.equals(awsAccessKey, that.awsAccessKey) &&
                Objects.equals(awsSecretKey, that.awsSecretKey) &&
                Objects.equals(logGroupName, that.logGroupName) &&
                Objects.equals(logStreamName, that.logStreamName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, sourceType, directoryPath, filePattern, scanIntervalSeconds,
                syslogPort, syslogProtocol, syslogFormat, httpEndpoint, httpAuthToken, requireAuth,
                awsRegion, awsAccessKey, awsSecretKey, logGroupName, logStreamName, queryRefreshIntervalSeconds);
    }

    @Override
    public String toString() {
        return "LogSourceConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", enabled=" + enabled +
                ", sourceType=" + sourceType +
                ", directoryPath='" + directoryPath + '\'' +
                ", filePattern='" + filePattern + '\'' +
                ", scanIntervalSeconds=" + scanIntervalSeconds +
                ", syslogPort=" + syslogPort +
                ", syslogProtocol='" + syslogProtocol + '\'' +
                ", syslogFormat='" + syslogFormat + '\'' +
                ", httpEndpoint='" + httpEndpoint + '\'' +
                ", httpAuthToken='" + (httpAuthToken != null ? "****" : null) + '\'' +
                ", requireAuth=" + requireAuth +
                ", awsRegion='" + awsRegion + '\'' +
                ", awsAccessKey='" + (awsAccessKey != null ? "****" : null) + '\'' +
                ", awsSecretKey='" + (awsSecretKey != null ? "****" : null) + '\'' +
                ", logGroupName='" + logGroupName + '\'' +
                ", logStreamName='" + logStreamName + '\'' +
                ", queryRefreshIntervalSeconds=" + queryRefreshIntervalSeconds +
                '}';
    }

    /**
     * Enum representing the type of log source.
     */
    public enum SourceType {
        FILE,
        SYSLOG,
        HTTP,
        CLOUDWATCH
    }
}