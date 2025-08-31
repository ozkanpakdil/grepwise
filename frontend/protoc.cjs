// Script to generate TypeScript code from proto files (CommonJS)
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Create directories if they don't exist
const genDir = path.join(__dirname, 'src', 'generated');
if (!fs.existsSync(genDir)) {
  fs.mkdirSync(genDir, { recursive: true });
}

// Clean generated directory before generating new files
console.log('Cleaning generated directory...');
fs.rmSync(genDir, { recursive: true, force: true });
fs.mkdirSync(genDir, { recursive: true });

// Proto files to process
const protoFiles = [
  '../src/main/proto/logservice.proto',
  '../src/main/proto/alarmservice.proto',
  '../src/main/proto/authservice.proto',
];

// Helper to resolve plugin path cross-platform (.cmd on Windows, binary on *nix)
function pluginPath(rel) {
  const base = path.join(__dirname, rel);
  if (process.platform === 'win32') return base + '.cmd';
  return base; // on linux/macos, the file is executable without .cmd
}

// Generate TypeScript code for each proto file
protoFiles.forEach((protoFile) => {
  const protoPath = path.join(__dirname, protoFile);
  const protoDir = path.dirname(protoPath);

  console.log(`Generating TypeScript code for ${protoFile}...`);

  try {
    const grpcWebPlugin = pluginPath('./node_modules/.bin/protoc-gen-grpc-web');

    // Use only grpc-web output to avoid conflicts
    const cmd = [
      'protoc',
      `--plugin=protoc-gen-grpc-web="${grpcWebPlugin}"`,
      `--grpc-web_out=import_style=typescript,mode=grpcwebtext:${genDir}`,
      `-I "${protoDir}"`,
      `"${protoPath}"`,
    ].join(' ');

    execSync(cmd, { stdio: 'inherit' });

    console.log(`Successfully generated TypeScript code for ${protoFile}`);
  } catch (error) {
    console.error(`Error generating TypeScript code for ${protoFile}:`, error.message);
    process.exit(1);
  }
});

console.log('TypeScript code generation completed successfully!');
