#!/usr/bin/env node

const http = require('http');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const MAX_RETRIES = 30;
const RETRY_DELAY = 2000;

function checkServer(url, retries = 0) {
  return new Promise((resolve, reject) => {
    const parsedUrl = new URL(url);
    const options = {
      hostname: parsedUrl.hostname,
      port: parsedUrl.port || 8080,
      path: '/actuator/health',
      method: 'GET',
      timeout: 5000
    };

    const req = http.request(options, (res) => {
      if (res.statusCode === 200) {
        console.log(`✓ Server is ready at ${BASE_URL}`);
        resolve();
      } else if (retries < MAX_RETRIES) {
        console.log(`Server returned ${res.statusCode}, retrying... (${retries + 1}/${MAX_RETRIES})`);
        setTimeout(() => checkServer(url, retries + 1).then(resolve).catch(reject), RETRY_DELAY);
      } else {
        reject(new Error(`Server returned ${res.statusCode} after ${MAX_RETRIES} retries`));
      }
    });

    req.on('error', (err) => {
      if (retries < MAX_RETRIES) {
        console.log(`Connection failed, retrying... (${retries + 1}/${MAX_RETRIES})`);
        setTimeout(() => checkServer(url, retries + 1).then(resolve).catch(reject), RETRY_DELAY);
      } else {
        reject(new Error(`Server at ${BASE_URL} is not responding after ${MAX_RETRIES} retries. Please start the application first with: java -jar target/*.jar`));
      }
    });

    req.on('timeout', () => {
      req.destroy();
      if (retries < MAX_RETRIES) {
        console.log(`Request timeout, retrying... (${retries + 1}/${MAX_RETRIES})`);
        setTimeout(() => checkServer(url, retries + 1).then(resolve).catch(reject), RETRY_DELAY);
      } else {
        reject(new Error(`Server health check timed out after ${MAX_RETRIES} retries`));
      }
    });

    req.end();
  });
}

console.log(`Checking if server is ready at ${BASE_URL}...`);
checkServer(BASE_URL)
  .then(() => {
    console.log('Server check passed. Running tests...');
    process.exit(0);
  })
  .catch((err) => {
    console.error('\n❌ Server check failed:');
    console.error(err.message);
    console.error('\nPlease ensure the application is running before executing tests.');
    console.error('Start the application with: java -jar target/*.jar\n');
    process.exit(1);
  });
