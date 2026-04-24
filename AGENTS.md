* If the user input looks like a bash command they have probably forgotten to escape it and you should stop and ask for guidance.
* If the user input looks incomplete ask the user and wait for the next message.
* If you are uncertain about user intentions clarify with the user and do not make assumptions.
* If the user input is a pasted log file, or similar, analyse it and ask the user for next steps; do not immediately start work.
* If the code is different to what you expect it is likely that the user has changed it deliberately and may have not committed the changes: do not revert it but ask the user what to do.
* Consult the user freely who is expert in this project unless you are specifically instructed to work autonomously.
* The project is a complex historical code base which is in the process of being migrated into a managed development workflow in stages.
* The project has recently been refactored into a multi-module maven project.
* You should assume there are mistakes and inconsistencies between code and documentation and that the codebase is not complete.
* A longer term goal is to migrate the project towards test driven development with fully documented design.
* Design artefacts will be in the `design` folder, with a meaningful snake-case filename.
* Keep a record of when you last looked at individual design artefacts, and look for recent modifications to the files. Expect them to change unpredictably without a clean git commit record.
* If you are in a git worktree or branch starting `scoder` you are in a sandbox and you may want to check for changes from upstream.
* If you are in a git worktree or branch starting `scoder` commit a set of changes (if any) after every round of conversation.
* Design artefact templates are in the `design/templates` folder - use these when populating design documents.
* The `design/overview.md` provides a background to the code structure and is a good entry point for orientation.
* The `design/priorities.md` is collaboratively authored with user and provides a list of current priorities, outstanding issues and technical debt. Value effort trade off is used for deciding on the next priority to implement.
* The `design/news.md` provides a high level list of changes and should be updated in each major commit.
* The `design/adr/` folder should contain architectural decision records as a set of markdown files one per decision.
* The `design/methods/` folder should contain record of scientific methods employed or considered in the project as a set of markdown files or Rmarkdown files.
* The `design/implementation/scoping` folder should contain a detailed description of the planned implementation of tests, issues or features from the `design/priorities.md` file.
* The `design/implementation/in-progress` folder should contain actively worked on implementations.
* The `design/implementation/complete` folder should contain old implementation check-lists for issues or features in the `design/news.md` file.
* Before any git commits update relevant design documents with changes.
* It is **mandatory** to use test driven development when implementing new features.
* All tests must be implemented and proven to fail before starting development of a feature.
* All tests must be proven to pass before development of a feature is complete.
* Tests should be directed at the success criteria not the implementation. Failing tests for existing functionality are expected.
* The `scratch` directory is designed for running end to end tests and allows working with simulation output.
* This design and implementation process is not expected to be linear. Possible high level processes to follow are:
  1. `discuss priorities` -> `scope implementation` -> `agree implementation with user` -> (repeat) `write tests` -> `implement feature` -> (until) `meets success criteria` -> `update design`
  2. `discuss priorities` -> `explore codebase and do research` -> `update methods or adr` -> (optional) `add emerging issues to priorities`
  3. `discuss priorities` -> `explore design and do research` -> `agree scope with user` -> (repeat) `update prioritised tests` -> (until) `test coverage satisfactory` -> (optional) `add emerging issues to priorities`
* The file `design/conventions.md` records stylistic conventions to adhere to when creating code or documentation.
* You should use paths relative to your home directory in case it changes.
