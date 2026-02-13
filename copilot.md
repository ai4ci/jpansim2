# Copilot Notes — Coding Style and Important Paths

Purpose
-------

This note collects the coding conventions, javadoc rules, significant
file locations and CI/test commands that future chat sessions or a human
developer should know when working on `jpansim2`.

Keep this short and practical; the intention is to speed up future
conversations and to remind a developer of repository conventions and
where to find key files.

Coding style (Java)
-------------------

- Follow standard Java conventions: camelCase for methods/fields, Pascal
 Case for classes and interfaces, and 4-space indentation.
- Wrap at 80 characters.
- Prefer immutability for small value objects where reasonable.
- Use modern Java idioms (streams, Optional) sparingly and only when it
 improves clarity.
- Keep methods short: prefer extracting private helper methods when a
 method grows beyond ~60-80 lines.

Javadoc rules (project-wide)
----------------------------

- Write Javadoc only for public classes and public methods, interface
  default public methods, and public static fields.
- Use British English spelling in Javadoc and plain ASCII (latin) text
  and punctuation only.
- Author tag: use `@author Rob Challen` in public class/method javadocs.
  Do not specify `@version`.
- Keep class-level Javadoc focussed on the main purpose and features of
  the class; avoid long digressions.
- For methods:
  - If the method behaviour is straightforward, document salient
    behaviour and important pre/post-conditions (no formulae).
  - If the method performs complex mathematical operations, include
    essential formulae. Use simple LaTeX surrounders: inline `\( ... \)`
    or block `\[ ... \]`. Prefer block equations, avoid aligned
    environments. Keep expressions minimal and suitable for MathJax.
- Include short notes in Javadoc linking to downstream uses where
  relevant (for example, which other modules or site pages depend on the
  class). Use plain URLs or relative references where helpful.

Significant files and locations
-------------------------------

- Top-level repository root: this is the Maven reactor root and the
  usual place to invoke `mvn` for aggregate tasks.
- Parent `pom.xml`: `jpansim2/pom.xml` — defines common properties,
  aggregation and the `distributionManagement` `site` configuration that
  uses `${session.executionRootDirectory}` by default.
- Core module: `jpansim2/jpansim2-core/` — main sources, tests and the
  module that produces the distributable `-jar-with-dependencies.jar`.
- Release workflow: `.github/workflows/release.yml` (or similar) — builds
  the module, copies artefacts to the repo-root `staging/` and creates a
  GitHub Release.
- Staging directory used by CI: `staging/` at the repository root (this
  is where CI copies jars before upload).
- Site staging path: default is `${session.executionRootDirectory}/target/staging`.

CI and release notes
--------------------

- Workflows trigger on annotated tag pushes matching `v*.*.*`.
- Important workflow details:
  - Use `working-directory:` on steps that must run in a submodule;
    `cd` inside a `run:` does not persist across steps.
  - Copy the produced `jpansim2-core/*-jar-with-dependencies.jar` into
    a repo-root `staging/` directory before upload.
  - `actions/upload-artifact` and `softprops/action-gh-release` expect
    paths relative to the workspace (repo root) by default.
- Site staging: prefer running `mvn site:stage` from the repo root so
  that `${session.executionRootDirectory}` points at the repo root and
  aggregation happens in one place.

Renaming artefacts
------------------

CI renames the `-jar-with-dependencies.jar` to a simpler `jpansim2-<ver>.jar`.
Use the robust bash snippet below in workflows or scripts to do the
rename reliably (no external `sed`):

```bash
shopt -s nullglob
for old in staging/*-jar-with-dependencies.jar; do
  base=$(basename -- "$old")
  ver=${base#jpansim2-core-}
  ver=${ver%-jar-with-dependencies.jar}
  mv -- "$old" "staging/jpansim2-${ver}.jar"
done
```

Useful commands
---------------

From repo root (recommended):

```bash
# clean, build and stage site to repo-root/target/staging
mvn -B -DskipTests clean package site site:stage

# list staged site
ls -la target/staging
```

Run site staging from a child module but target the repo root:

```bash
mvn site:stage -Dsite.staging.dir=/absolute/path/to/repo/target/staging
```

Tagging and releasing
---------------------

Use an annotated tag to trigger the release workflow:

```bash
git tag -a v1.2.3 -m "Release jpansim2 v1.2.3"
git push origin v1.2.3
```

Debugging CI when uploads fail
-----------------------------

Add a debug step in the workflow to list files and show the workspace
path before upload:

```yaml
- name: Debug staging content
  run: |
    echo "Workspace: $GITHUB_WORKSPACE"
    ls -la
    ls -la staging || true
```

Notes for future chat sessions
-----------------------------

- Useful places to open first: `jpansim2/pom.xml`,
  `jpansim2/jpansim2-core/pom.xml`, `.github/workflows/release.yml`, and
  `jpansim2/dev-notes.md`.
- If asked about staging paths, explain the distinction between the
  invocation directory and the reactor root; mention
  `${session.executionRootDirectory}` and the CLI override
  `-Dsite.staging.dir=...`.
- If asked to modify workflows, prefer `working-directory:` over `cd`
  and ensure a repo-root `staging/` directory exists before upload.

Last updated: 2026-02-13
