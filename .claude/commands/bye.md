---
description: End session with context optimization and save
tags: [session, workflow, gitignored]
---

# End Session Protocol

You are ending a work session. Follow these steps in order:

## 1. Optimize Context
- Review current todos and mark any completed items
- Create/update session summary in `claudedocs/` directory if significant work was done
- Document any key decisions, issues resolved, or important context
- Update relevant roadmap/progress documents with checkboxes

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

⏭️  Next session:
- [next tasks]

🌙 Rest well! Your progress is saved and ready to pick up exactly where you left off.

Use `/hello` when you return! 👋
```

Keep it warm, concise, and motivating!
