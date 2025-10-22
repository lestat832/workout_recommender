# Global Commands Update - October 21, 2025

## What Changed

Updated global `/hello` and `/bye` commands in `~/.claude/commands/` to use Serena MCP tools instead of `/sc:load` and `/sc:save`.

## Why This Matters

**Before:**
- Global commands used `/sc:load` and `/sc:save`
- Each project needed custom commands to use Serena MCP
- Command override issues when project commands lacked `name:` field

**After:**
- Global commands use Serena MCP tools directly
- All future projects get Serena session management by default
- No need for project-specific command overrides
- Consistent session management across all projects

## Changes Made

### Global `/hello` (`~/.claude/commands/hello.md`)

**Changed:**
```markdown
## 2. Load Session
Execute the `/sc:load` command to restore previous session context
```

**To:**
```markdown
## 2. Load Session
Execute the following Serena MCP tools to restore session context:
- `mcp__serena__check_onboarding_performed` - Check if project is onboarded
- `mcp__serena__list_memories` - List available session memories
- Run `git status` and `git branch` to check repository state
```

### Global `/bye` (`~/.claude/commands/bye.md`)

**Changed:**
```markdown
## 3. Save Session
Execute the `/sc:save` command to persist session state
```

**To:**
```markdown
## 3. Save Session Memory
Use Serena MCP to save session state:
- `mcp__serena__write_memory` with memory name like `session-YYYY-MM-DD-[brief-description]`
- Include in memory:
  - What was accomplished
  - Current state/progress
  - Next steps planned
  - Important decisions or context
```

## Benefits

1. **Automatic Setup**: New projects automatically get Serena session management
2. **No Override Issues**: All projects use the same commands consistently
3. **Better Memory**: Serena provides richer session context than `/sc:save`
4. **Project-Agnostic**: Works across all registered Serena projects

## How It Works

### Session Start (`/hello`)
1. Checks if project is onboarded with Serena
2. Lists available session memories from `.serena/memories/`
3. Loads most recent session context
4. Shows git status and branch
5. Provides session briefing with where you left off

### Session End (`/bye`)
1. Reviews and completes todos
2. Commits and pushes git changes
3. Saves session memory to `.serena/memories/session-YYYY-MM-DD-[description].md`
4. Provides friendly goodbye with accomplishments summary

## Project Setup Requirements

For any project to use these commands, it needs:

1. **Serena Registration**: Project path in `~/.serena/serena_config.yml`
   ```yaml
   projects:
   - /Users/marcgeraldez/Projects/your_project
   ```

2. **Serena Directory**: `.serena/memories/` directory structure
   ```bash
   mkdir -p .serena/memories
   ```

3. **Gitignore**: Keep memories local-only
   ```
   # .serena/.gitignore
   memories/
   ```

4. **Permissions** (optional): Add to `.claude/settings.local.json`
   ```json
   {
     "permissions": {
       "allow": [
         "Read(//Users/marcgeraldez/Projects/your_project/.serena/**)"
       ]
     }
   }
   ```

## Migration Path

### Existing Projects Using `/sc:load` and `/sc:save`

**Option 1: Migrate to Serena** (Recommended)
1. Register project in `~/.serena/serena_config.yml`
2. Create `.serena/memories/` directory
3. Add `.serena/.gitignore`
4. Remove project-specific `/hello` and `/bye` commands
5. Use global commands automatically

**Option 2: Keep Project-Specific Commands**
1. Keep existing project commands with `name:` field
2. Project commands override global ones
3. Continue using `/sc:load` and `/sc:save`

### New Projects

1. Register project: Add to `~/.serena/serena_config.yml`
2. Create directory: `mkdir -p .serena/memories`
3. Start using: `/hello` and `/bye` work immediately
4. First memory: Created automatically on first `/bye`

## Comparison: `/sc:save` vs Serena MCP

| Feature | `/sc:save` | Serena MCP |
|---------|-----------|------------|
| Storage | `claudedocs/SESSION_STATE.md` | `.serena/memories/session-*.md` |
| Naming | Fixed filename | Dated, descriptive names |
| History | Overwrites previous | Keeps full history |
| Memory List | Manual file search | `mcp__serena__list_memories` |
| Context Loading | Read single file | Smart context retrieval |
| Multi-Project | Per-project file | Centralized registry |
| Gitignore | Committed to repo | Local-only by default |

## Testing

Verified working in:
- ✅ `workout_app` - Primary test project
- 🔄 `nova_scholartrail` - Already using Serena
- ⏳ Future projects - Will work automatically

## Rollback

If needed, revert global commands to use `/sc:load` and `/sc:save`:

1. Edit `~/.claude/commands/hello.md`:
   ```markdown
   ## 2. Load Session
   Execute the `/sc:load` command to restore previous session context
   ```

2. Edit `~/.claude/commands/bye.md`:
   ```markdown
   ## 3. Save Session
   Execute the `/sc:save` command to persist session state
   ```

---

**Updated**: October 21, 2025
**Impact**: All current and future projects
**Status**: ✅ Complete and tested
