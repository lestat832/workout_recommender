---
name: hello
description: Start session with friendly greeting and context load
tags: [session, workflow, gitignored, project]
---

# Start Session Protocol

You are beginning a new work session. Follow these steps in order:

## 1. Warm Greeting
Provide a friendly, energetic welcome message

## 2. Load Previous Session Context
Use Serena MCP tools to restore previous session state:

```javascript
// List available session memories
const memories = await mcp__serena__list_memories()

// Read relevant session context
const sessionMemory = await mcp__serena__read_memory({ memory_file_name: 'current_session.md' })
const recentWork = await mcp__serena__read_memory({ memory_file_name: 'recent_work.md' })
```

**If memories are found:**
- Restore previous session context
- Identify where we left off
- Load task progress

**If no memories found:**
- This is a fresh start
- Initialize new session context

## 2b. Smart Reference Loading
**Purpose**: Load only the references needed for today's work

After session loads, help the user identify which references to load:

### Reference Loading Guide
Ask the user what they're working on, then suggest relevant references:

**Reference Map**:
```
Working on...                    → Load these references:
------------------------------------------------------------------
ViewModels, Compose UI, Room     → android-patterns.md
Workout generation, cooldown     → workout-domain.md
Strava OAuth, sync queue         → strava-integration.md
Gym setup, equipment filtering   → gym-equipment.md
CSV import, Strong app data      → data-import.md
UI components, wolf theming      → ui-components.md
```

**Loading Instructions**:
1. User tells you their focus area
2. You identify relevant reference(s)
3. Inform user: "I'll load the [name] reference for you"
4. Internally note which reference patterns to use (no actual file loading needed - just awareness)

**Example interaction**:
```
User: "I want to work on the workout generation algorithm"
You: "Perfect! I'll keep the workout-domain.md reference in mind - it has the
     GenerateWorkoutUseCase implementation, 7-day cooldown logic, and equipment
     filtering priority rules. What specifically do you want to improve?"

User: "I need to build a new onboarding screen"
You: "Great! I'll reference ui-components.md and android-patterns.md for this.
     These cover Compose patterns, wolf theming, and the FTUE onboarding flow.
     What should this onboarding screen do?"
```

**Multiple References**: If work spans multiple areas, acknowledge all relevant references:
```
User: "Add Strava sync status to the workout cards"
You: "I'll reference both strava-integration.md (for sync status indicators)
     and ui-components.md (for WorkoutCard component patterns). Let's start..."
```

**Pro tip**: The core CLAUDE.md is always loaded. References are loaded on-demand only when needed for specific implementation work.

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
