# Redaction Configuration Guide

GrepWise can redact sensitive data from search results and alerts. By default it redacts values following common keys like password and passwd. You can customize the behavior by editing a JSON file and (optionally) adding your own regex patterns.

Config file location:
- Linux/macOS: ~/.GrepWise/config/redaction.json
- Windows: C:\Users\<you>\.GrepWise\config\redaction.json

If the file does not exist, it will be created automatically at startup with default keys ["password","passwd"] and seeded with five default password/passwd patterns. The grouped-map format is required; if a legacy flat redaction.json is found, the app will migrate it to the grouped format on startup.

## JSON formats

Grouped-map format (required; keys grouped with their own patterns):

```
{
  "[\"password\",\"passwd\"]": {
    "patterns": [
      "(?i)(secret\s*[:=]\s*)([^\n\r\t ]+)",
      "(?i)(api[_-]?key\s*[:=]\s*)([^\n\r]+)"
    ]
  },
  "cardnumber": {
    "patterns": [
      "(?i)(card(number)?\s*[:=]\s*)(\b(?:\d[ -]*?){13,19}\b)"
    ]
  }
}
```

Notes:
- JSON keys must be strings; for multi-key groups, the property name is a JSON-stringified array (as shown above).
- At runtime, all keys across groups are flattened for redaction keyword detection; all patterns across groups are combined.

- keys: Case-insensitive keyword names. Keys are used for detection and for masking metadata values when the key name itself is sensitive. They do not auto-generate value-masking patterns; provide patterns explicitly under the appropriate group.
- patterns: Provide the exact regexes you want to use for masking. Use parentheses capturing groups:
  - Group(1) captures the non-sensitive prefix (e.g., "token=", "secret: ") preserved in output.
  - Group(2) captures the sensitive value that will be replaced by the mask.
  - If your pattern has no groups, the entire match will be masked.
  - Tip: You can include the five common key-based forms for your keys (JSON style, key=value, key: value, key -> value, and bare "key value") directly in the patterns list under the appropriate group.

The default mask is:
- Search/results/export: *****
- Alerts (OpsGenie/PagerDuty): ***

## Examples

Below are ready-to-use examples you can copy into the patterns array.

### 1) Telephone numbers
- Very permissive (digits, spaces, dashes, parentheses, plus sign) after a key:
  - Key-based (already handled if you add keys like phone, telephone, mobile):
    - Add to keys: ["phone", "telephone", "mobile"]
  - Custom pattern (prefix+value):
    - "(?i)(phone\s*[:=]\s*)([+()\-\d\s]{6,})"
    - "(?i)(telephone\s*[:=]\s*)([+()\-\d\s]{6,})"

### 2) Credit card numbers
- 13–19 digits (with optional spaces or dashes) after a key:
  - Add to keys: ["card", "cardnumber", "cc", "creditcard"]
  - Or custom pattern:
    - "(?i)(card(number)?\s*[:=]\s*)(\b(?:\d[ -]*?){13,19}\b)"

Note: This does not validate using Luhn; it’s a format-oriented redaction.

### 3) ID number
- Example generic national ID after a key:
  - Add to keys: ["id", "idnumber", "nationalid"]
  - Or custom pattern:
    - "(?i)(id(number)?\s*[:=]\s*)([A-Za-z0-9\-]{6,})"

### 4) Bearer tokens, API keys, secrets
- Examples:
  - "(?i)(authorization\s*:\s*Bearer\s+)([^\n\r]+)"
  - "(?i)(api[_-]?key\s*[:=]\s*)([^\n\r]+)"
  - "(?i)(secret\s*[:=]\s*)([^\n\r\t ]+)"

## Managing at runtime

- Get current config (groups plus convenience flattened keys/patterns):
  - GET /api/redaction/config
- Update config (grouped format only; flat {keys,patterns} is rejected):
  - POST /api/redaction/config with body like:
    {
      "[\"password\",\"passwd\"]": { "patterns": [
        "(\\\"(?i:password|passwd)\\\"\\s*:\\s*)\\\"([^\\\\\\\"]*)\\\"",
        "((?i:password|passwd)\\s*[=:]\\s*)([^\\\\n\\\\r\\\\t ]+)",
        "((?i:password|passwd)\\s*:\\s*)([^\\\\n\\\\r]+)",
        "((?i:password|passwd)\\s*->\\s*)([^\\\\n\\\\r]+)",
        "((?i:password|passwd)\\b)(\\s+)([^\\\\n\\\\r]+)"
      ]},
      "cardnumber": { "patterns": [
        "(?i)(card(number)?\\s*[:=]\\s*)(\\b(?:\\d[ -]*?){13,19}\\b)"
      ]}
    }
- Legacy endpoints (still work):
  - GET /api/redaction/keys
  - POST /api/redaction/reload

## Tips
- Always leave password/passwd present; the backend forces these defaults even if omitted.
- Prefer keys where possible; they automatically get several common formats supported.
- For custom patterns, prefer two capture groups (prefix in group 1, sensitive value in group 2) for best results.
- Test your regexes (e.g., on regex101.com) before adding to production.
