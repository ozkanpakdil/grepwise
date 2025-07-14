# SPL Query Language Guide for GrepWise

## Overview

GrepWise now supports an enhanced query language similar to Splunk's SPL (Search Processing Language). This allows for more powerful and flexible log analysis with commands that can be chained together using pipes (`|`).

## Basic Syntax

SPL queries follow this pattern:
```
search <criteria> | command1 <args> | command2 <args> | ...
```

## Supported Commands

### 1. search
The `search` command is used to find log entries matching specific criteria.

**Examples:**
```spl
search error
search "database connection"
search level=ERROR
search source=app.log
```

### 2. where
The `where` command filters results based on field conditions.

**Examples:**
```spl
search * | where level=ERROR
search * | where source=app.log
search * | where message="connection failed"
```

### 3. stats
The `stats` command performs statistical operations on the data.

**Examples:**
```spl
search * | stats count
search * | stats count by level
search * | stats count by source
search error | stats count by level
```

### 4. sort
The `sort` command orders results by specified fields.

**Examples:**
```spl
search * | sort timestamp
search * | sort level
search * | sort -timestamp    # descending order
search * | sort -level        # descending order
```

### 5. head
The `head` command limits results to the first N entries.

**Examples:**
```spl
search * | head 10
search error | head 5
search * | sort -timestamp | head 1    # most recent entry
```

### 6. tail
The `tail` command limits results to the last N entries.

**Examples:**
```spl
search * | tail 10
search error | tail 5
search * | sort timestamp | tail 1     # oldest entry
```

### 7. eval
The `eval` command creates or modifies fields (basic implementation).

**Note:** Currently has limited functionality - planned for future enhancement.

## Complex Query Examples

### Find Recent Errors
```spl
search level=ERROR | sort -timestamp | head 10
```

### Count Errors by Source
```spl
search level=ERROR | stats count by source
```

### Find Database-Related Issues
```spl
search database | where level=ERROR | sort -timestamp
```

### Get Error Statistics
```spl
search * | where level=ERROR | stats count by source
```

### Recent Warning Messages
```spl
search level=WARN | sort -timestamp | head 5
```

### Complex Analysis Pipeline
```spl
search * | where level=ERROR | sort -timestamp | head 20 | stats count by source
```

## API Usage

### Endpoint
```
POST /api/logs/spl
Content-Type: text/plain
```

### Request Body
Send the SPL query as plain text in the request body.

### Response
The response format depends on the query type:

**For log entries (search, where, sort, head, tail):**
```json
[
  {
    "id": "1",
    "timestamp": 1234567890,
    "level": "ERROR",
    "message": "Database connection failed",
    "source": "app.log",
    "metadata": {},
    "rawContent": "..."
  }
]
```

**For statistics (stats command):**
```json
{
  "ERROR": 15,
  "WARN": 8,
  "INFO": 42
}
```

### Example cURL Commands

**Simple search:**
```bash
curl -X POST http://localhost:8080/api/logs/spl \
  -H "Content-Type: text/plain" \
  -d "search error"
```

**Statistics query:**
```bash
curl -X POST http://localhost:8080/api/logs/spl \
  -H "Content-Type: text/plain" \
  -d "search * | stats count by level"
```

**Complex query:**
```bash
curl -X POST http://localhost:8080/api/logs/spl \
  -H "Content-Type: text/plain" \
  -d "search level=ERROR | sort -timestamp | head 10"
```

## Field Reference

### Available Fields
- `level`: Log level (ERROR, WARN, INFO, DEBUG, etc.)
- `message`: Log message content
- `source`: Log source file or system
- `timestamp`: Log entry timestamp
- `id`: Unique log entry identifier

### Field Usage in Commands
- **search**: `search field=value`
- **where**: `where field=value`
- **sort**: `sort field` or `sort -field`
- **stats**: `stats count by field`

## Tips and Best Practices

1. **Start with search**: Always begin queries with a `search` command
2. **Use specific searches**: More specific search criteria improve performance
3. **Chain commands logically**: Order commands from broad to specific filtering
4. **Limit results**: Use `head` or `tail` to limit large result sets
5. **Use stats for analysis**: The `stats` command is powerful for log analysis

## Future Enhancements

The following features are planned for future releases:
- Enhanced `eval` command with field calculations
- Additional statistical functions (avg, sum, min, max)
- Time-based grouping and analysis
- Regular expression support in where clauses
- Custom field extraction
- More advanced sorting options

## Error Handling

If a query contains invalid syntax or unknown commands:
- Unknown commands are logged as warnings but don't stop execution
- Invalid field references default to safe values
- Malformed queries return appropriate error messages

## Performance Considerations

- Use specific search criteria to reduce the initial dataset
- Apply filtering (`where`) early in the pipeline
- Limit results with `head`/`tail` when appropriate
- Statistics operations are performed in memory, so be mindful of large datasets