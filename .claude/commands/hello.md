---
description: Start session with friendly greeting and context load
tags: [session, workflow, gitignored]
---

# Start Session Protocol

You are beginning a new work session. Follow these steps in order:

## 1. Warm Greeting
Provide a friendly, energetic welcome message

## 2. Load Session
Execute the `/sc:load` command to restore previous session context

## 3. Session Briefing
After loading completes, provide:
- Quick recap of where we left off
- Current status/progress
- What's next on the agenda
- Ask what the user wants to work on

**Example hello format:**
```
👋 Welcome back!

🔄 Loading your previous session...

[after load completes]

📍 Where we left off:
- [brief context]

✅ Completed: [X/Y tasks]
⏳ In Progress: [current phase]

🎯 Ready to continue with:
- [next logical task]

What would you like to work on today?
```

Keep it energetic, clear, and action-oriented!
