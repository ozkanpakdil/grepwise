// Script to generate TypeScript code from proto files
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Create directories if they don't exist
const genDir = path.join(__dirname, 'src', 'generated');
if (!fs.existsSync(genDir)) {
  fs.mkdirSync(genDir, { recursive: true });
}

// Proto files to process
const protoFiles = [
  '../src/main/proto/logservice.proto',
  '../src/main/proto/alarmservice.proto',
  '../src/main/proto/authservice.proto'
];

// Generate TypeScript code for each proto file
protoFiles.forEach(protoFile => {
  const protoPath = path.join(__dirname, protoFile);
  const protoDir = path.dirname(protoPath);
  
  console.log(`Generating TypeScript code for ${protoFile}...`);
  
  try {
    // Run protoc with the TypeScript and gRPC-Web plugins
    execSync(`protoc \
      --plugin=protoc-gen-ts=./node_modules/.bin/protoc-gen-ts.cmd \
      --plugin=protoc-gen-grpc-web=./node_modules/.bin/protoc-gen-grpc-web.cmd \
      --js_out=import_style=commonjs,binary:${genDir} \
      --ts_out=service=grpc-web:${genDir} \
      --grpc-web_out=import_style=typescript,mode=grpcwebtext:${genDir} \
      -I ${protoDir} \
      ${protoPath}`);
    
    console.log(`Successfully generated TypeScript code for ${protoFile}`);
  } catch (error) {
    console.error(`Error generating TypeScript code for ${protoFile}:`, error.message);
    process.exit(1);
  }
});

console.log('TypeScript code generation completed successfully!');