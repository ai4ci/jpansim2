# Developer Notes — Build, Staging and Release

Last updated: 2026-02-13

These notes capture the supported local and CI workflows for building,
staging the site and publishing releases for jpansim2. The CI workflows
automate most steps, but you will still tag commits to create versioned
releases.

Overview
--------

- Build artefacts are produced by the module `jpansim2-core`.
- The parent `pom.xml` config uses `${session.executionRootDirectory}` so a
  top-level Maven invocation produces a single shared `target/staging`
  directory for the full reactor.
- GitHub Actions handle packaging, staging and release steps.

Local build and site staging
----------------------------

For the site: from the repo root:

```bash
# clean, build and stage site to repo-root/target/staging
mvn -B -DskipTests clean package site site:stage
# inspect staged site
ls -la target/staging
```

To build artefacts locally from `jpansim2-core` directory

```bash
# run inside jpansim2/jpansim2-core
mvn -B -DskipTests clean package
```

Packaging and renaming the distributable jar
-------------------------------------------

CI builds produce a `-jar-with-dependencies.jar` for `jpansim2-core`.
We copy and rename that file into a repo-root `staging/` directory so the
release/upload steps find a consistent filename, e.g. `jpansim2-1.2.3.jar`.

GitHub Actions pages deployment
-------------------------------

- Checks out repository
- Sets up Java and caches Maven dependencies.
- Calls mvn `-B -DskipTests clean package site site:stage` from parent root with target
directory `target/staging`
- Copies `target/staging`
- JavaDoc generation is not aggregated but run in individual root packages. Have to
make sure reportSets do not execute `test-javadoc` as this fails

GitHub Actions release workflow (high level)
-------------------------------------------

The GitHub Actions workflow triggers on tags matching `v*.*.*` and will
build, stage and upload the jar before creating a GitHub Release.

```bash
# create annotated tag and push can be done after successful pages deployment
git tag -a v1.2.3 -m "Release jpansim2 v1.2.3"
git push origin v1.2.3
```

Key points the workflow does:

- Checks out the repository.
- Sets up Java and caches Maven dependencies.
- Runs Maven in the `jpansim2-core` module (use `working-directory:` or
  `-f`/`-pl`) to produce the jar-with-dependencies.
- Copies produced jar(s) to a repo-root `staging` directory and renames
  them to `jpansim2-${version}.jar`.
- Uses `actions/upload-artifact` and `softprops/action-gh-release` to
  attach the file(s) to the GitHub release and/or upload them as workflow
  artifacts.

- Each `run:` step runs in a fresh shell. Use `working-directory:` on a
  step to run commands from a non-root directory persistently for that
  step. Do not rely on `cd` in one `run:` step to affect following steps.
- `actions/upload-artifact` and `softprops/action-gh-release` operate on
  paths relative to the workspace (the repo root) unless you change the
  step `working-directory`.

Troubleshooting
---------------

- Problem: `upload-artifact` cannot find the jar.
  - Cause: the jar was not copied to repo-root `staging` (or the step used
    a different `working-directory`).
  - Fix: Ensure the `cp` copies to the repo-root staging path and add a
    debug listing step.

- Problem: Site staging wrote to a child module `target/staging`.
  - Cause: Maven was invoked in the child directory so
    `${session.executionRootDirectory}` equals the child project.
  - Fix: Run Maven from repo root or pass `-Dsite.staging.dir=...`.

- Problem: Javadoc or aggregated site files missing from staged site.
  - Cause: plugin configuration or lifecycle ordering. Ensure the modules
    are built and `site` runs at the top level so aggregation happens.
    Run `mvn -DskipTests clean package site site:stage` from the root and
    inspect `target/staging` to confirm the expected files.


