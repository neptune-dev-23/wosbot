# WosBot Web UI

A React + TypeScript interface for monitoring and managing the Whiteout Survival bot. It provides real-time log streaming, profile insights, task scheduling visibility, and basic bot control actions within a modern dashboard.

## Highlights

- Live logs: search, filter, paginate, and auto-scroll through server-sent log events.
- Profiles view: inspect emulator assignments, server association, and runtime status per profile.
- Tasks view: see schedules, execution state, and upcoming run times with visual urgency cues.
- Bot controls: issue pause, resume, stop, or start actions directly from the UI.
- Responsive layout: collapsible navigation and mobile-friendly design for remote access.

## Tech Stack

- Bun 1.3+ for package management, scripts, and hot reload (bun --hot).
- Vite 7 + React 19 for fast local development and production builds.
- TypeScript for type-safe frontend logic.
- Tailwind-style utility classes blended with bespoke CSS for styling.
- Server-Sent Events (SSE) connections to the Spring backend for logs and bot state.

## Development Quick Start

`
bun install
bun run dev  # starts Vite on http://localhost:8000 (proxying API calls)
`

Create a .env with any extras (e.g., ALLOWED_DEV_HOSTS=your-host) based on .env.example.

## Build & Deploy

`
bun run build
`

The build copies hashed assets into ../wos-web/src/main/resources/static/. Spring Boot then serves the React bundle directly, and deep links (e.g., /logs, /profiles, /tasks) resolve through a static fallback.

## Additional Scripts

- bun run lint – ESLint checks.
- bun run preview – preview the production build locally.
- bun run build – type-check, Vite production bundle, and asset sync.


