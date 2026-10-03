# Contract: Agent Bash Guard

**Feature**: `002-agentic-engineering` · Source: [research](../research.md) R2, R3

## Interface

- File: `.claude/hooks/clmp-bash-guard.mjs` (Node ≥ 20, no dependencies)
- Invocation: from the `PreToolUse` hook (matcher `Bash`) in the reviewer and validator agent
  frontmatter, using the repository-relative path, e.g.
  `node .claude/hooks/clmp-bash-guard.mjs reviewer`
- Input: hook JSON on stdin. Only `tool_input.command` is read.
- Output: exit `0` = allow (no output). Exit `2` = block, with a single-line reason on stderr
  starting `clmp-bash-guard:`. Malformed input or an unknown mode also blocks (exit `2`).
- It never executes or rewrites the command.

## Rules

1. **Single command**. Block if the command contains any of the following: newline, `;`, `&&`,
   `||`, `|`, `` ` ``, `$(`, `${`, `<`, `>`, or a trailing `&`. The one exception is a single
   trailing `2>&1`, which is stripped before the other checks.
2. **Allowlist by mode**. The command, after trimming and collapsing whitespace, must match one of
   these:

| Mode | Allowed forms |
|------|---------------|
| `reviewer` | `git diff …`, `git log …`, `git show …`, `git status …`, `git rev-parse …`, `git merge-base …`, `git ls-files …`, `git blame …`, `git branch --list …` / `git branch -a` / `git branch --show-current` |
| `validator` | everything in `reviewer`, **plus**: `java -version`, `node -v`, `npm -v`; `./backend/mvnw …` or `backend/mvnw …` where the arguments include `-f backend/pom.xml` and the goals are only `test`, `verify`, or `-q` combined with `-Dtest=…`, `-Dgroups=…`, `-DfailIfNoTests=false`, `-Dsurefire.failIfNoSpecifiedTests=false`; `npm --prefix frontend run typecheck`, `npm --prefix frontend run build`, `npm --prefix frontend test -- --run [pattern]`; `npm --prefix frontend ci` |

3. **Argument checks**. In both modes, `git` arguments must not include `-c`, `--output`,
   `--exec`, or `-o`. For `git diff`/`log`/`show`, an `--output=` flag blocks. Maven arguments
   must not include `clean`, `install`, `deploy`, `spring-boot:run`, `-D…=` keys other than those
   listed, or `-s`/`--settings`.
4. **Block message**: `clmp-bash-guard: <mode> may not run "<first 80 chars>" — report the need
   in your report instead.`

## Test cases (run during implementation; see quickstart Q-V1)

| Mode | Command | Expected |
|------|---------|----------|
| reviewer | `git diff main...HEAD` | allow |
| reviewer | `git diff main...HEAD --stat 2>&1` | allow |
| reviewer | `git checkout main` | block |
| reviewer | `git diff && rm -rf x` | block |
| reviewer | `touch x` | block |
| reviewer | `./backend/mvnw -f backend/pom.xml verify` | block |
| validator | `./backend/mvnw -f backend/pom.xml verify` | allow |
| validator | `./backend/mvnw -q -f backend/pom.xml test -Dtest=UnlinkedRecruiterScopeIT` | allow |
| validator | `./backend/mvnw -f backend/pom.xml clean install` | block |
| validator | `npm --prefix frontend test -- --run` | allow |
| validator | `npm --prefix frontend install lodash` | block |
| validator | `sed -i s/a/b/ backend/pom.xml` | block |
| validator | `cd backend && ./mvnw verify` | block |
| any | `echo hi > f` | block |
| unknown | `git status` | block |

`npm ci` writes only to the ignored `frontend/node_modules/`; Maven writes only to the ignored
`backend/target/`. Neither counts as modifying the repository (spec edge case "Build output and
caches").
