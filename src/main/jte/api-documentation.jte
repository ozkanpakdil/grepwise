<!-- No imports or parameters needed for static content -->

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GrepWise API Documentation</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
        }
        h1 {
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 10px;
        }
        h2 {
            color: #2980b9;
            margin-top: 30px;
        }
        .endpoint {
            background-color: #f8f9fa;
            border-left: 4px solid #3498db;
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 0 4px 4px 0;
        }
        .method {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 4px;
            font-weight: bold;
            margin-right: 10px;
        }
        .get {
            background-color: #61affe;
            color: white;
        }
        .post {
            background-color: #49cc90;
            color: white;
        }
        .put {
            background-color: #fca130;
            color: white;
        }
        .delete {
            background-color: #f93e3e;
            color: white;
        }
        .path {
            font-family: monospace;
            font-size: 1.1em;
            margin-bottom: 10px;
        }
        .description {
            margin-bottom: 10px;
        }
        .parameters {
            font-family: monospace;
            background-color: #eee;
            padding: 10px;
            border-radius: 4px;
        }
        .intro {
            margin-bottom: 30px;
        }
    </style>
</head>
<body>
    <h1>GrepWise API Documentation</h1>
    
    <div class="intro">
        <p>This page provides documentation for the GrepWise API, an open-source alternative to Splunk for log analysis and monitoring.</p>
        <p>The API allows you to search logs, execute SPL queries, and retrieve log data in various formats.</p>
    </div>
    
    <h2>API Endpoints</h2>
    
    <!-- API endpoints are hardcoded for simplicity -->
    
    <div class="endpoint">
        <div>
            <span class="method get">GET</span>
            <span class="path">/api/logs/search</span>
        </div>
        <div class="description">Search logs with query parameters</div>
        <div class="parameters">query, startTime, endTime, limit</div>
    </div>
    
    <div class="endpoint">
        <div>
            <span class="method post">POST</span>
            <span class="path">/api/logs/ingest</span>
        </div>
        <div class="description">Ingest logs via API</div>
        <div class="parameters">logs (array of log entries)</div>
    </div>
    
    <h2>Response Formats</h2>
    <p>All API endpoints return JSON responses unless otherwise specified.</p>
    
    <h2>Authentication</h2>
    <p>Authentication is required for all API endpoints. Use the authentication token in the Authorization header.</p>
    
    <h2>Rate Limiting</h2>
    <p>API requests are limited to 100 requests per minute per user.</p>
    
    <footer>
        <p>GrepWise API Documentation - Version 1.0.0</p>
    </footer>
</body>
</html>