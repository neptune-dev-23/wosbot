# Status Page Documentation

## Overview
The Status page provides a comprehensive overview of all profiles and their task statuses in a clean, modern interface.

## Features

### Real-time Updates
- Automatically refreshes every 30 seconds
- Shows last update time in the header

### Profile Cards
Each profile is displayed in its own card showing:
- Profile name and emulator number
- Active/Inactive status indicator
- Grid of important tasks

### Task Information
For each task, the following information is displayed:
- **Task Name**: The name of the task (Intel, Gather, Alliance Help, etc.)
- **Last Execution**: How long ago the task was last executed
  - Shows "Just now" for recent executions
  - Shows "Xm ago" for minutes
  - Shows "Xh XXm ago" for hours
  - Shows "Xd XXh ago" for days
  - Red color if too old (>1 hour)
- **Next Schedule**: When the task is scheduled to run next
  - Shows "Ready now" in green if the task is ready
  - Shows countdown timer format (HH:MM:SS)
  - Orange color for distant schedules (>2 hours)

### Design Features
- Clean, modern card-based layout
- Hover effects on cards
- Color-coded status indicators:
  - Green: Active/Ready
  - Red: Inactive/Overdue
  - Orange: Warning/Distant
  - Gray: Normal/Info
- Responsive design that works with different window sizes
- Professional gradient header
- Smooth animations and transitions

### Important Tasks Monitored
- Intelligence (Intel)
- Gather Resources
- Alliance Help
- Alliance Chests
- Crystal Laboratory
- Hero Recruitment
- Life Essence
- Mail Rewards

## Usage
1. Click the "Status" button in the main application
2. View real-time status of all profiles
3. Monitor task execution times and schedules
4. Page automatically refreshes every 30 seconds

## Benefits
- Quick overview of all bot activity
- Easy identification of stuck or overdue tasks
- Professional presentation of data
- No need to manually refresh - updates automatically
