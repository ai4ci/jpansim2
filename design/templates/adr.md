# {{TITLE e.g. LOCAL CLI language — Node.js + bun}}

**Status**: {{Proposed|Accepted|Rejected|Deprecated}}

## Context

{{e.g. The LOCAL CLI needs to manage async processes (SSH tunnel child process, heartbeat timer, stdin for user input), handle SSH subprocess execution, and potentially evolve into a lightweight API routing server.}}

## Decision 

{{e.g. Implement the LOCAL CLI in Node.js using bun as the runtime and package manager.}}

## Rationale

{{e.g.
- Bun provides fast startup, built-in TypeScript support, and a single-binary distribution — good for a CLI tool.
- Node.js has mature primitives for managing child processes (`child_process.spawn`), async I/O, and timers, all needed for the session-owner pattern.
- Aligns with the future routing server direction (an HTTP server is trivial to add).
- LOGIN and COMPUTE scripts remain plain bash — no runtime dependency on the HPC side.
}}

## Consequences

{{e.g. Requires bun installed on LOCAL. Not natively portable to Windows (out of MVP scope).}}