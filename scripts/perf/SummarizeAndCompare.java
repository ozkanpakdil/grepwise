import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Summarize JMeter CSV results, compare to moving average baseline, update history and badge.
 *
 * Mirrors the previous Python summarizer functionality using only core Java APIs.
 *
 * Usage (Java source-file mode):
 *   java scripts/perf/SummarizeAndCompare.java
 *
 * Outputs:
 *   - target/jmeter/perf-summary.json
 *   - target/jmeter/perf-summary.md
 *   - docs/perf/history.csv (appended)
 *   - docs/perf/badge.svg
 *
 * Exit code:
 *   - 2 when any scenario exceeds BLOCK threshold (>20% p95 regression), 0 otherwise.
 */
public class SummarizeAndCompare {
    // Config paths
    static final Path RESULTS_DIR = Paths.get("target/jmeter/results");
    static final Path OUT_DIR = Paths.get("target/jmeter");
    // Persist history and badge under build output; the workflow will publish these to gh-pages.
    static final Path HISTORY_DIR = OUT_DIR;
    static final Path HISTORY_FILE = HISTORY_DIR.resolve("history.csv");
    static final Path SUMMARY_JSON = OUT_DIR.resolve("perf-summary.json");
    static final Path SUMMARY_MD = OUT_DIR.resolve("perf-summary.md");
    static final Path SUMMARY_HTML = OUT_DIR.resolve("perf-summary.html");
    static final Path BADGE_SVG = OUT_DIR.resolve("badge.svg");

    // Env
    static final String RUN_NUMBER = System.getenv().getOrDefault("GITHUB_RUN_NUMBER", "");
    static final String SHA = Optional.ofNullable(System.getenv("GITHUB_SHA")).map(s -> s.length() >= 7 ? s.substring(0, 7) : s).orElse("");
    static final String BRANCH = System.getenv().getOrDefault("GITHUB_REF_NAME", "");
    static final String NOW_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC).format(Instant.now());

    // Thresholds
    static final double WARN_PCT = 5.0;
    static final double ALERT_PCT = 10.0;
    static final double BLOCK_PCT = 20.0;

    public static void main(String[] args) throws Exception {
        ensureDirs();

        List<Path> csvFiles;
        if (Files.isDirectory(RESULTS_DIR)) {
            try (var s = Files.list(RESULTS_DIR)) {
                csvFiles = s.filter(p -> p.toString().endsWith(".csv")).sorted().collect(Collectors.toList());
            }
        } else {
            csvFiles = List.of();
        }

        if (csvFiles.isEmpty()) {
            System.out.println("No JMeter CSV results found under " + RESULTS_DIR);
            System.exit(0);
        }

        Map<String, Agg> scenarios = new LinkedHashMap<>();
        // Collect per-endpoint samples across all CSVs
        Map<String, List<Sample>> endpointSamples = new LinkedHashMap<>();
        for (Path p : csvFiles) {
            String raw = stripExtension(p.getFileName().toString());
            String name = friendlyScenarioName(raw);
            List<Sample> samples = readJMeterCsv(p);
            // Merge endpoint samples
            for (Sample s : samples) {
                endpointSamples.computeIfAbsent(s.label, k -> new ArrayList<>()).add(s);
            }
            Agg agg = aggregate(samples);
            // Only keep non-empty scenarios
            if (!Double.isNaN(agg.p95_ms)) {
                scenarios.put(name, agg);
            }
        }

        List<HistRow> history = readHistory();

        StringBuilder md = new StringBuilder();
        md.append("# GrepWise Performance Summary\n");
        md.append(String.format("- Run: %s  Commit: `%s`  Branch: `%s`  Time: %s\n\n", RUN_NUMBER, SHA, BRANCH, NOW_ISO));
        md.append("| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |\n");
        md.append("|---|---:|---:|---:|---:|---:|:--:|\n");

        // Prepare HTML summary as well
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html>\n<meta charset=\"utf-8\">\n<title>GrepWise Performance Summary</title>\n");
        html.append("<style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans,sans-serif;line-height:1.5;padding:20px} table{border-collapse:collapse;margin:1em 0;width:100%} th,td{border:1px solid #ddd;padding:6px 8px} th{background:#f6f8fa;text-align:left} code{background:#f6f8fa;padding:2px 4px;border-radius:4px}</style>\n");
        html.append("<h1>GrepWise Performance Summary</h1>\n");
        html.append(String.format(Locale.US, "<p>Run: %s &nbsp;&nbsp; Commit: <code>%s</code> &nbsp;&nbsp; Branch: <code>%s</code> &nbsp;&nbsp; Time: %s</p>", RUN_NUMBER, SHA, BRANCH, NOW_ISO));
        html.append("<h2>Scenarios</h2>\n");
        html.append("<table><thead><tr><th>Scenario</th><th>p95 (ms)</th><th>Δ vs avg</th><th>Avg (ms)</th><th>Throughput (req/s)</th><th>Errors (%)</th><th>Status</th></tr></thead><tbody>\n");

        String worstLevel = "green";
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("generatedAt", NOW_ISO);
        json.put("run", RUN_NUMBER);
        json.put("commit", SHA);
        json.put("branch", BRANCH);
        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("warnPct", WARN_PCT);
        thresholds.put("alertPct", ALERT_PCT);
        thresholds.put("blockPct", BLOCK_PCT);
        json.put("thresholds", thresholds);
        Map<String, Map<String, Object>> jsonScenarios = new LinkedHashMap<>();

        boolean shouldBlock = false;
        for (var entry : scenarios.entrySet()) {
            String name = entry.getKey();
            Agg agg = entry.getValue();
            List<Double> histP95 = history.stream()
                    .filter(h -> name.equals(h.scenario) && !Double.isNaN(h.p95_ms) && h.p95_ms > 0)
                    .map(h -> h.p95_ms)
                    .collect(Collectors.toList());
            double baseline = movingAverage(histP95, 10);
            double curP95 = agg.p95_ms;
            double delta = Double.NaN;
            String level = "green";
            String emoji = "✅";
            String note = "";

            boolean hasData = !Double.isNaN(curP95);
            if (!hasData) {
                note = "no data";
            } else if (!Double.isNaN(baseline) && baseline > 0) {
                delta = ((curP95 - baseline) / baseline) * 100.0;
                if (delta > BLOCK_PCT) {
                    level = "red"; emoji = "🛑"; note = "BLOCK (>20%)"; shouldBlock = true;
                } else if (delta > ALERT_PCT) {
                    level = "red"; emoji = "🔴"; note = "RED (>10%)";
                } else if (delta > WARN_PCT) {
                    level = "yellow"; emoji = "🟡"; note = "YELLOW (>5%)";
                } else { level = "green"; emoji = "✅"; note = "OK"; }
            } else {
                note = "no baseline";
            }

            worstLevel = worseOf(worstLevel, level);

            Map<String, Object> js = new LinkedHashMap<>();
            js.put("avg_ms", numOrNull(agg.avg_ms));
            js.put("p95_ms", numOrNull(agg.p95_ms));
            js.put("throughput", numOrNull(agg.throughput));
            js.put("err_rate", numOrNull(agg.err_rate));
            js.put("count", agg.count);
            js.put("errors", agg.errors);
            js.put("baseline_p95_ms", numOrNull(baseline));
            js.put("delta_pct_vs_baseline_p95", numOrNull(delta));
            js.put("level", level);
            js.put("note", note);
            jsonScenarios.put(name, js);

            md.append(String.format(Locale.US,
                    "| %s | %s | %s | %s | %s | %s | %s %s |\n",
                    name,
                    fmtNum(agg.p95_ms, "%.1f"),
                    fmtNum(delta, "%.1f%%"),
                    fmtNum(agg.avg_ms, "%.1f"),
                    fmtNum(agg.throughput, "%.2f"),
                    fmtNum(agg.err_rate, "%.2f"),
                    emoji, note));

            // Also add HTML row
            html.append(String.format(Locale.US,
                    "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s %s</td></tr>\n",
                    escapeHtml(name),
                    escapeHtml(fmtNum(agg.p95_ms, "%.1f")),
                    escapeHtml(fmtNum(delta, "%.1f%%")),
                    escapeHtml(fmtNum(agg.avg_ms, "%.1f")),
                    escapeHtml(fmtNum(agg.throughput, "%.2f")),
                    escapeHtml(fmtNum(agg.err_rate, "%.2f")),
                    emoji, escapeHtml(note)));
        }

        // Close scenarios table in HTML
        html.append("</tbody></table>\n");

        // Endpoints tested table (aggregated by label across all CSVs)
        Map<String, Agg> endpointsAgg = new LinkedHashMap<>();
        for (var e : endpointSamples.entrySet()) {
            endpointsAgg.put(e.getKey(), aggregate(e.getValue()));
        }
        // Sort endpoints by count desc
        List<Map.Entry<String, Agg>> endpointList = new ArrayList<>(endpointsAgg.entrySet());
        endpointList.sort((a,b) -> Integer.compare(b.getValue().count, a.getValue().count));

        // Helpful descriptions for scenarios
        md.append("\n### About scenarios\n\n");
        md.append("- HTTP Search: exercises the REST search endpoints over HTTP.\n");
        md.append("- Syslog UDP: sends log events into the UDP syslog ingestion path.\n");
        md.append("- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.\n\n");
        html.append("<h2>About scenarios</h2><ul><li>HTTP Search: exercises the REST search endpoints over HTTP.</li><li>Syslog UDP: sends log events into the UDP syslog ingestion path.</li><li>Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.</li></ul>\n");

        if (!endpointList.isEmpty()) {
            md.append("## Endpoints tested (by JMeter label)\n\n");
            md.append("| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |\n");
            md.append("|---|---:|---:|---:|---:|\n");
            html.append("<h2>Endpoints tested (by JMeter label)</h2><table><thead><tr><th>Endpoint</th><th>Avg (ms)</th><th>p95 (ms)</th><th>Count</th><th>Errors (%)</th></tr></thead><tbody>\n");
        }
        Map<String, Map<String, Object>> jsonEndpoints = new LinkedHashMap<>();
        for (var e : endpointList) {
            String label = e.getKey();
            Agg a = e.getValue();
            md.append(String.format(Locale.US, "| %s | %s | %s | %d | %s |\n",
                    label,
                    fmtNum(a.avg_ms, "%.1f"),
                    fmtNum(a.p95_ms, "%.1f"),
                    a.count,
                    fmtNum(a.err_rate, "%.2f")));
            html.append(String.format(Locale.US, "<tr><td>%s</td><td>%s</td><td>%s</td><td>%d</td><td>%s</td></tr>\n",
                    escapeHtml(label),
                    escapeHtml(fmtNum(a.avg_ms, "%.1f")),
                    escapeHtml(fmtNum(a.p95_ms, "%.1f")),
                    a.count,
                    escapeHtml(fmtNum(a.err_rate, "%.2f"))));
            Map<String,Object> je = new LinkedHashMap<>();
            je.put("avg_ms", numOrNull(a.avg_ms));
            je.put("p95_ms", numOrNull(a.p95_ms));
            je.put("count", a.count);
            je.put("err_rate", numOrNull(a.err_rate));
            jsonEndpoints.put(label, je);
        }
        if (!endpointList.isEmpty()) {
            html.append("</tbody></table>\n");
        }

        String badgeColor = "#4c1";
        String badgeText = "OK";
        if ("yellow".equals(worstLevel)) { badgeColor = "#dfb317"; badgeText = "WARN"; }
        else if ("red".equals(worstLevel)) { badgeColor = "#e05d44"; badgeText = "ALERT"; }
        makeBadge(badgeColor, badgeText);

        Map<String, Object> overall = new LinkedHashMap<>();
        overall.put("level", worstLevel);
        overall.put("badge", badgeText);
        json.put("scenarios", jsonScenarios);
        json.put("overall", overall);
        json.put("endpoints", jsonEndpoints);

        // Write summary files
        writeString(SUMMARY_JSON, toJson(json));
        writeString(SUMMARY_MD, md.append('\n').toString());
        writeString(SUMMARY_HTML, html.toString());

        // Append to history
        List<String> toAppend = new ArrayList<>();
        String header = "timestamp,run,commit,branch,scenario,avg_ms,p95_ms,throughput,err_rate";
        if (!Files.exists(HISTORY_FILE)) {
            ensureDir(HISTORY_DIR);
            writeString(HISTORY_FILE, header + System.lineSeparator());
        }
        for (var e : scenarios.entrySet()) {
            Agg a = e.getValue();
            String line = String.format(Locale.US,
                    "%s,%s,%s,%s,%s,%.3f,%.3f,%.5f,%.3f",
                    NOW_ISO, RUN_NUMBER, SHA, BRANCH, e.getKey(), a.avg_ms, a.p95_ms, a.throughput, a.err_rate);
            toAppend.add(line);
        }
        Files.write(HISTORY_FILE, (String.join(System.lineSeparator(), toAppend) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND);

        // Compact line for logs
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("overall", overall);
        Map<String, Object> sc = new LinkedHashMap<>();
        for (var e : jsonScenarios.entrySet()) {
            Map<String, Object> v = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("p95_ms", v.get("p95_ms"));
            m.put("delta_pct", v.get("delta_pct_vs_baseline_p95"));
            m.put("level", v.get("level"));
            sc.put(e.getKey(), m);
        }
        compact.put("scenarios", sc);
        System.out.println(toJson(compact));

        System.exit(shouldBlock ? 2 : 0);
    }

    static String worseOf(String a, String b) {
        if (a.equals(b)) return a;
        // red > yellow > green
        if ("red".equals(a) || "red".equals(b)) return "red";
        if ("yellow".equals(a) || "yellow".equals(b)) return "yellow";
        return "green";
    }

    static void ensureDirs() throws IOException {
        ensureDir(OUT_DIR);
        ensureDir(HISTORY_DIR);
    }

    static Object numOrNull(double d) {
        return (Double.isNaN(d) || Double.isInfinite(d)) ? null : d;
    }

    static String fmtNum(double d, String fmt) {
        return (Double.isNaN(d) || Double.isInfinite(d)) ? "-" : String.format(Locale.US, fmt, d);
    }

    static void ensureDir(Path p) throws IOException {
        if (!Files.exists(p)) Files.createDirectories(p);
    }

    static String stripExtension(String n) {
        int i = n.lastIndexOf('.');
        return i >= 0 ? n.substring(0, i) : n;
    }

    static String friendlyScenarioName(String raw) {
        String r = raw.toLowerCase(Locale.ROOT);
        if (r.contains("http-search")) return "HTTP Search";
        if (r.contains("syslog-udp")) return "Syslog UDP";
        if (r.contains("combined-parallel-bsh") || r.contains("combined-parallel")) return "Combined Parallel (BSH)";
        // Fallback: return original raw name
        return raw;
    }

    static class Sample { double elapsed; boolean success; String label; }

    static List<Sample> readJMeterCsv(Path path) throws IOException {
        List<Sample> out = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) return out;
            String[] cols = Arrays.stream(header.split(",")).map(String::trim).toArray(String[]::new);
            Map<String,Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) idx.put(cols[i], i);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = splitCsv(line, cols.length);
                try {
                    double elapsed = parseDouble(get(parts, idx, "elapsed"), get(parts, idx, "Elapsed"));
                    String successStr = get(parts, idx, "success");
                    if (successStr == null) successStr = get(parts, idx, "Success");
                    boolean success = successStr != null && successStr.equalsIgnoreCase("true");
                    String label = Optional.ofNullable(get(parts, idx, "label")).orElse(Optional.ofNullable(get(parts, idx, "Label")).orElse("unknown"));
                    Sample s = new Sample();
                    s.elapsed = elapsed; s.success = success; s.label = label;
                    out.add(s);
                } catch (Exception ignored) { }
            }
        }
        return out;
    }

    // Basic CSV splitting that handles simple quoted fields; adequate for JMeter outputs
    static String[] splitCsv(String line, int expected) {
        List<String> res = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                res.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        res.add(cur.toString());
        while (res.size() < expected) res.add("");
        return res.toArray(new String[0]);
    }

    static String get(String[] parts, Map<String,Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i < 0 || i >= parts.length) return null;
        return parts[i];
    }

    static double parseDouble(String... opts) {
        for (String s : opts) {
            if (s == null || s.isEmpty()) continue;
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        return 0.0;
    }

    static class Agg { double avg_ms, p95_ms, throughput, err_rate; int count, errors, success; }

    static Agg aggregate(List<Sample> samples) {
        Agg a = new Agg();
        if (samples.isEmpty()) { a.avg_ms = Double.NaN; a.p95_ms = Double.NaN; return a; }
        double[] el = samples.stream().mapToDouble(s -> s.elapsed).toArray();
        a.count = el.length;
        a.avg_ms = Arrays.stream(el).average().orElse(Double.NaN);
        a.p95_ms = percentile(el, 95);
        a.errors = (int) samples.stream().filter(s -> !s.success).count();
        a.success = a.count - a.errors;
        double totalElapsedSec = Arrays.stream(el).sum() / 1000.0;
        a.throughput = totalElapsedSec > 0 ? a.count / totalElapsedSec : 0.0;
        a.err_rate = a.count > 0 ? (a.errors * 100.0) / a.count : 0.0;
        return a;
    }

    static double percentile(double[] values, double p) {
        if (values.length == 0) return Double.NaN;
        double[] v = Arrays.copyOf(values, values.length);
        Arrays.sort(v);
        double k = (v.length - 1) * (p / 100.0);
        int f = (int) Math.floor(k);
        int c = (int) Math.ceil(k);
        if (f == c) return v[f];
        double d0 = v[f] * (c - k);
        double d1 = v[c] * (k - f);
        return d0 + d1;
    }

    static List<HistRow> readHistory() throws IOException {
        List<HistRow> hist = new ArrayList<>();
        if (!Files.exists(HISTORY_FILE)) return hist;
        try (BufferedReader br = Files.newBufferedReader(HISTORY_FILE, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) return hist;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                try {
                    HistRow h = new HistRow();
                    h.timestamp = parts[0];
                    h.run = parts.length > 1 ? parts[1] : "";
                    h.commit = parts.length > 2 ? parts[2] : "";
                    h.branch = parts.length > 3 ? parts[3] : "";
                    h.scenario = parts.length > 4 ? parts[4] : "";
                    h.avg_ms = parts.length > 5 ? Double.parseDouble(parts[5]) : Double.NaN;
                    h.p95_ms = parts.length > 6 ? Double.parseDouble(parts[6]) : Double.NaN;
                    h.throughput = parts.length > 7 ? Double.parseDouble(parts[7]) : 0.0;
                    h.err_rate = parts.length > 8 ? Double.parseDouble(parts[8]) : 0.0;
                    hist.add(h);
                } catch (Exception ignored) { }
            }
        }
        return hist;
    }

    static class HistRow {
        String timestamp, run, commit, branch, scenario; double avg_ms, p95_ms, throughput, err_rate;
    }

    static double movingAverage(List<Double> values, int window) {
        if (values == null || values.isEmpty()) return Double.NaN;
        int from = Math.max(0, values.size() - window);
        List<Double> sub = values.subList(from, values.size());
        return sub.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    static void makeBadge(String color, String label) throws IOException {
        String svg = "" +
                "<svg xmlns='http://www.w3.org/2000/svg' width='150' height='20'>\n" +
                "  <linearGradient id='b' x2='0' y2='100%'>\n" +
                "    <stop offset='0' stop-color='#bbb' stop-opacity='.1'/>\n" +
                "    <stop offset='1' stop-opacity='.1'/>\n" +
                "  </linearGradient>\n" +
                "  <mask id='a'>\n" +
                "    <rect width='150' height='20' rx='3' fill='#fff'/>\n" +
                "  </mask>\n" +
                "  <g mask='url(#a)'>\n" +
                "    <rect width='80' height='20' fill='#555'/>\n" +
                "    <rect x='80' width='70' height='20' fill='" + color + "'/>\n" +
                "    <rect width='150' height='20' fill='url(#b)'/>\n" +
                "  </g>\n" +
                "  <g fill='#fff' text-anchor='middle' font-family='Verdana,Geneva,DejaVu Sans,sans-serif' font-size='11'>\n" +
                "    <text x='40' y='14'>Perf</text>\n" +
                "    <text x='115' y='14'>" + label + "</text>\n" +
                "  </g>\n" +
                "</svg>\n";
        writeString(BADGE_SVG, svg);
    }

    static void writeString(Path path, String content) throws IOException {
        ensureDir(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // Minimal JSON serialization for our nested Maps/Lists/primitives
    static String toJson(Object o) {
        if (o == null) return "null";
        if (o instanceof String s) return '"' + escape(s) + '"';
        if (o instanceof Number || o instanceof Boolean) return String.valueOf(o);
        if (o instanceof Map<?,?> m) {
            return "{" + m.entrySet().stream().map(e -> toJson(String.valueOf(e.getKey())) + ":" + toJson(e.getValue())).collect(Collectors.joining(",")) + "}";
        }
        if (o instanceof Collection<?> c) {
            return "[" + c.stream().map(SummarizeAndCompare::toJson).collect(Collectors.joining(",")) + "]";
        }
        return '"' + escape(String.valueOf(o)) + '"';
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
