#  Task Manager - Android App

A premium, full-featured task management application built from scratch using Java and Android Studio. Designed for students and teams, this application helps you stay organized, collaborate effectively, track deadlines, and optimize productivity.

[![Android](https://img.shields.io/badge/Platform-Android-green?style=flat&logo=android)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-orange?style=flat&logo=java)](https://www.java.com/)

An Android task management application designed to help users organize tasks, receive timely notifications, and stay productive.

## Key Features

- **User Authentication**: Secure user registration and login with local SQLite database storage. Passwords are dynamically secured using SHA-256 with user-specific salting.
- **Profile Management**: View and modify user profile information (Full Name, Email) and change password securely. Includes a quick switch user feature for local debugging.
- **Project CRUD & Member Roles**:
  - Create and manage projects with description and status tracking.
  - Team collaboration: Project leaders can invite members to join a project, manage tasks, and remove members.
  - Role-based permissions logic distinguishing project leaders (with write/assign access) and project members.
- **Project Member Invitations (Accept/Reject Flow)**:
  - Inviting a user to a project sends a pending invitation.
  - Users can view pending invitations on their project list screen and choose to Accept or Reject them.
- **Task Management Upgrade**:
  - Link tasks to specific projects and assign them to project members.
  - Track tasks with due dates, priority tiers (LOW, MEDIUM, HIGH), and status tags (PENDING, IN_PROGRESS, COMPLETED).
  - Dynamic full-text search, multi-faceted filtering (by project, assignee, priority, status), and sorting options (due date, priority, status).
  - File attachments support per task.
- **Collaborative Comments**: Add and view comments under specific tasks, promoting group communication and task-level feedback.
- **Activity Logs**: Immutable task audit logs tracing the history of task creation, edits, state updates, assignee changes, and comments.
- **Notification & Alarm System**:
  - Background foreground service (`NotificationService`) checks upcoming tasks and schedules real-time alarms.
  - Full-screen `AlarmActivity` triggering ringtone alerts for critical overdue tasks.
  - Notification delivery is contextually filtered: users are notified only of tasks they are assigned to or tasks within projects they belong to.
  - Service automatically recovers after device reboot (`BootReceiver`).
- **Premium Dashboard**: Visual summary statistics tracking total projects, total tasks, completed tasks, pending tasks, and overdue tasks with a completion progress bar.
- **Calendar View (Phase 7)**:
  - Custom month-grid calendar view defaulting to the active date (June 2026 for coursework).
  - Highlights dates with deadlines using priority-colored indicator dots (Red for high, Yellow for medium, Green for low).
  - Dynamic filtering of tasks when clicking a specific date on the grid.
- **System-wide Light/Dark Mode**: Integrated toggling of application themes via Profile settings. User preferences are persisted in SharedPreferences and apply instantly across all activities.

## Technical Stack & Architecture

- **Platform & Language**: Java, Android SDK 26 (Android 8.0) to SDK 33 (Android 13).
- **Architecture**: Clean, modular packaging structure using Repository Pattern, SQLite databases, and Fragment-based navigation.
- **Database**: SQLite with `TaskDbHelper`. Foreign keys enabled (`ON DELETE CASCADE`), database schema migrated up to version 7 (adding users, projects, members, task revisions, comments, logs, and invitations).
- **Background Processing**: `AlarmManager`, Foreground Services, and `BroadcastReceivers`.
- **UI Styling**: Material Design components, dynamic layouts, Custom Grid Calendars, and SharedPreferences-backed automatic theme switching.

## Permissions

- `INTERNET`: For fetching motivational quotes dynamically (with offline fallback).
- `RECEIVE_BOOT_COMPLETED`: Automatically launches background notifications check upon system boot.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_DATA_SYNC`: Runs the persistent deadline reminder monitor.
- `POST_NOTIFICATIONS`: Requests runtime push notifications permission on Android 13+.
- `SCHEDULE_EXACT_ALARM`: Triggers precise full-screen alarm activities when tasks become overdue.
- `USE_FULL_SCREEN_INTENT`: Displays alarm notifications on lock screens.
- `WAKE_LOCK`: Powers on display temporarily for urgent task deadline alarms.
- `VIBRATE`: Provides haptic feedback during alarms.
- `READ_EXTERNAL_STORAGE` & `WRITE_EXTERNAL_STORAGE` (API <= 32) & `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` (API >= 33): For adding attachments to tasks.

## Project Structure

```
app/src/main/java/me/zubair/taskmanager/
├── activity/       # Application activities (MainActivity, LoginActivity, RegisterActivity, AlarmActivity)
├── adapter/        # RecyclerView adapters (TaskAdapter, ProjectAdapter, ProjectMemberAdapter, CommentAdapter, ActivityLogAdapter, InvitationAdapter)
├── database/       # Schema definitions, contracts, SQLite helpers, and repository logic
├── fragments/      # Fragment screens (HomeFragment, TaskListFragment, ProjectListFragment, CalendarFragment, ProfileFragment, details and dialog screens)
├── model/          # Structured data models (Task, User, Project, ProjectMember, Comment, ActivityLog, Invitation)
├── preferences/    # Key-value persistent preferences (UserPreferencesManager)
├── receivers/      # Broadcast receivers (BootReceiver, AlarmReceiver)
├── services/       # Background execution services (NotificationService)
└── utils/          # Utilities (FileHelper, QuoteApiClient, HashUtils)
```

## Setup Instructions

1. Clone the repository.
2. Open the directory in **Android Studio**.
3. Allow Gradle synchronization to complete.
4. Run/deploy on an Android Emulator or physical device running Android 8.0 (API 26) or above.

## Screenshots






