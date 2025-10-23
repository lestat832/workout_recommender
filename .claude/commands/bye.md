---
name: bye
description: End session with context optimization and save
tags: [session, workflow, gitignored, project]
---

# End Session Protocol

You are ending a work session. Follow these steps in order:

## 1. Optimize Context
- Review current todos and mark any completed items
- Summarize the session's work and key decisions
- Document any important context that should persist
- **CRITICAL**: Never comment out `@MCP_Serena.md` in `.claude/CLAUDE.md` - it's required for session lifecycle (`/sc:load`, `/sc:save`) and will break future sessions

## 1b. Daily Context Health Check
**Purpose**: Maintain optimal context size and identify issues

Run these checks every session:

### File Size Monitoring
```bash
# Check CLAUDE.md size
wc -c .claude/CLAUDE.md

# Check reference file sizes
find .claude/references -name "*.md" -exec wc -c {} \;
```

**Size Guidelines**:
- ✅ CLAUDE.md <15K: Optimal
- ⚠️ CLAUDE.md 15K-20K: Consider extraction
- 🚨 CLAUDE.md >20K: Immediate extraction needed
- ⚠️ Reference files >10K: Consider splitting

### Deprecated Pattern Detection
```bash
# Check for outdated patterns in CLAUDE.md
grep -i "deprecated\|obsolete\|old\|legacy" .claude/CLAUDE.md

# Check for broken reference links
grep "@.claude/references/" .claude/CLAUDE.md
```

### Health Report Format
Present results as:
```
✅ Context health: Optimal
  - CLAUDE.md: 11.2K (optimal)
  - All references: <8K (good)
  - No deprecated patterns found
```

Or if issues found:
```
⚠️ Context health warnings:
  - CLAUDE.md: 18.5K (approaching limit)
  - gym-equipment.md: 11.2K (consider splitting)
  - 2 deprecated patterns found in workout-domain.md
```

## 1c. Monthly Deep Maintenance Audit
**Purpose**: Comprehensive context health analysis every 30 sessions

### Session Counter Management
```bash
# Read current session count (via Serena)
read_memory("context_health_session_count")

# Increment by 1
write_memory("context_health_session_count", "[current + 1]")

# If count >= 30: trigger deep audit, reset to 0
```

### Deep Audit Checklist (when count >= 30)
Run comprehensive analysis:

1. **Reference File Bloat Detection**
   ```bash
   # Flag files >500 lines
   find .claude/references -name "*.md" -exec wc -l {} \; | awk '$1 > 500'
   ```

2. **Duplicate Pattern Detection**
   - Search for repeated code examples across references
   - Identify overlapping explanations
   - Flag redundant "When to load" triggers

3. **Framework Version Audit**
   - Check for outdated library versions in android-patterns.md
   - Verify Kotlin version matches current project
   - Update Compose BOM version if needed

4. **Cross-Reference Validation**
   - Verify all `@.claude/references/` links in CLAUDE.md exist
   - Check for orphaned reference files
   - Validate "When to load" instructions are accurate

5. **Unused Pattern Detection**
   - Review Serena memory for reference loading frequency
   - Flag references never/rarely loaded (< 5% of sessions)
   - Consider merging low-use references

6. **Generate Audit Report**
   ```markdown
   # Context Health Audit Report
   **Date**: [current date]
   **Sessions since last audit**: 30

   ## Summary
   - Files analyzed: 7 (CLAUDE.md + 6 references)
   - Total context size: [XX]K tokens
   - Issues found: [count]

   ## Findings
   ### 🚨 Critical
   - [Critical issues requiring immediate action]

   ### ⚠️ Warnings
   - [Non-urgent but should address]

   ### ✅ Healthy
   - [Aspects working well]

   ## Recommendations
   1. [Action item 1]
   2. [Action item 2]

   ## Next Audit
   Due after 30 more sessions (approximately [date estimate])
   ```

7. **Reset Counter and Update Audit Date**
   ```bash
   write_memory("context_health_session_count", "0")
   write_memory("context_health_last_deep_audit", "[current date]")
   ```

**Note**: Only run deep audit when session count >= 30. Otherwise, just increment the counter.

## 2. Commit and Push Changes
If there are any changes in the git working directory:
- Run `git status` to review changes
- Stage all relevant files (modified files, new files, updated documentation)
- Create a descriptive commit message following the project's commit style
- Include co-authorship: `Co-Authored-By: Claude <noreply@anthropic.com>`
- Use the format with HEREDOC for proper message formatting
- Push to remote repository (GitHub)

**Commit and push format:**
```bash
git add [relevant files]
git commit -m "$(cat <<'EOF'
[type]: [description]

[optional detailed changes]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)" && git push
```

## 3. Save Session
Execute the `/sc:save` command to persist session state

## 4. Friendly Goodbye
Provide a warm, encouraging sign-off message that includes:
- Brief summary of what was accomplished
- What's queued up for next session
- Encouraging message

**Example goodbye format:**
```
🎉 Great work today!

✅ Completed:
- [key accomplishments]

💾 Session context saved and ready for next time

⏭️  Next session:
- [next tasks]

🌙 Rest well! Your progress is saved and ready to pick up exactly where you left off.

Use `/hello` when you return! 👋
```

Keep it warm, concise, and motivating!
