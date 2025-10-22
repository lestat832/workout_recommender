---
name: hello
description: Start session with friendly greeting and context load
tags: [session, workflow, gitignored, project]
---

# Start Session Protocol

You are beginning a new work session. Follow these steps in order:

## 1. Warm Greeting
Provide a friendly, energetic welcome message

## 2. Load Session
Execute the following Serena MCP tools to restore session context:
- `mcp__serena__check_onboarding_performed` - Check if project is onboarded
- `mcp__serena__list_memories` - List available session memories
- Run `git status` and `git branch` to check repository state

## 3. Session Briefing
After loading completes, provide:
- Quick recap of where we left off (from memories if available)
- Current status/progress
- What's next on the agenda
- Ask what the user wants to work on

**Example hello format:**
```
👋 Welcome back!

🔍 Loading your previous session...

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
