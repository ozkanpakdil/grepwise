Great! Here's a high-quality AI prompt series for bootstrapping the UI side of a Splunk-like log analyzer. The chosen tech stack is:

* **Frontend**: React (with TypeScript)
* **UI Framework**: Tailwind CSS + shadcn/ui (or Material UI as a fallback)
* **State Management**: React Query (for data fetching) + Zustand (for local UI state)
* **Search & Query Interface**: Monaco Editor (like in VSCode) for search input
* **Backend (mocked or real)**: REST or gRPC interface (for future extension)

---

### 🎯 Prompt #1 – Project Initialization

```
You are a senior full-stack developer. Create a new frontend project using React and TypeScript. Use Vite for fast build tooling. Integrate Tailwind CSS for styling and install shadcn/ui for reusable components. Configure the project with clean folder structure for scalability. Scaffold the project with:

- Pages folder (for routes like /search, /alarms)
- Components folder (reusable components)
- API folder (for future API requests)
- Store folder (Zustand or other state management)
- Utilities folder (helper functions)

Output full terminal commands and key folder/file content to bootstrap the project.
```

---

### 🎯 Prompt #2 – Implement the Search UI

```
You are building a search interface like Splunk. Users should be able to write search queries (e.g., `error OR warning | sort by timestamp desc`) in an advanced input. Use Monaco Editor to create a code-like search bar. Below the editor, show a table of logs matching the query.

- Use mocked data for now (JSON logs with timestamp, level, message)
- Use React Query for search fetch simulation
- Add a loading spinner and error message states
- Make the table sortable and paginated
- Use Tailwind CSS + shadcn/ui for styling

Provide component code, styling, and mock logic.
```

---

### 🎯 Prompt #3 – Display Log Details in a Side Drawer

```
Extend the log result table: when a row is clicked, open a side drawer showing detailed info of that log entry (e.g., full JSON, related metadata). Use shadcn/ui Drawer or a custom component with Tailwind.

- Show nicely formatted JSON (use prism.js or similar)
- Include “Copy to Clipboard” and “View in Fullscreen” buttons
- Keep drawer responsive for mobile and desktop

Provide full code for the drawer and integration with the log table.
```

---

### 🎯 Prompt #4 – Create & Manage Alarms from Search

```
Allow users to save their current search as an alarm. Add a “Save as Alarm” button above the search results. Clicking it opens a modal form where the user can:

- Name the alarm
- Set trigger condition (e.g., “> 10 errors in 5 mins”)
- Choose notification method (email, webhook, etc. – just mock these)

Save alarms in localStorage for now. Also provide a page (/alarms) where saved alarms are listed with options to delete or edit them.

Provide React components, modal, and logic for saving/loading alarms.
```

---

### 🎯 Prompt #5 – Add Navigation & Basic Authentication

```
Add a simple navigation bar with links to:
- Log Search
- Alarms
- Settings (future use)

Add a basic mock login page (no real auth yet). Use Zustand to store user state. Redirect to login if not authenticated.

Structure the app using React Router. Keep routes protected using a guard component.

Provide code for the layout, navigation, login, and route protection.
```

---

### ✅ Optional Prompt – Polish the UI

```
Improve the overall UI/UX of the app:

- Use proper colors for log levels (e.g., red for ERROR, yellow for WARNING)
- Use badges or icons for better readability
- Make mobile responsive
- Add dark/light theme toggle using shadcn/ui theme system

List improvements made and provide updated components.
```

---

