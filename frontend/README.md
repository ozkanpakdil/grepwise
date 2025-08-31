# GrepWise Frontend

This is the frontend for the GrepWise project, an open-source alternative to Splunk. It's built with React, TypeScript,
and Vite.

## Tech Stack

- **Frontend**: React with TypeScript
- **UI Framework**: Tailwind CSS + shadcn/ui
- **State Management**: React Query (for data fetching) + Zustand (for local UI state)
- **Search & Query Interface**: Monaco Editor (like in VSCode) for search input
- **Backend Communication**: gRPC

## Project Structure

- `src/pages`: For routes like /search, /alarms
- `src/components`: Reusable components
- `src/api`: API requests to the backend
- `src/store`: Zustand state management
- `src/utils`: Helper functions
- `src/lib`: Shared libraries and utilities
- `src/styles`: Global styles

## Getting Started

### Prerequisites

- Node.js (v16 or later)
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start the development server
npm run dev
```

## Features

- Advanced log search interface
- Log visualization
- Alarm creation and management
- User authentication