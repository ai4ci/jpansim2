* If the user input looks like a bash command they have probably forgotten to escape it and you should stop and ask whether they forgot to escape it.
* If the user input looks incomplete ask the user and wait for the next message.
* If you are uncertain about user intentions clarify with the user and do not make assumptions.
* Consult the user freely unless you are specifically instructed to work autonomously.
* The project is a complex historical code base which is in the process of being integrated into a agentic development workflow in stages.
* The project has recently been refactored into a multi-module maven project
* You should assume there are mistakes and inconsistencies between code and documentation and that the codebase is not complete.
* A longer term goal is to move the project towards test driven design with fully documented design and roadmap.
* Initial priorities are to improve testing and document existing features.
* Additionally we are aiming to add specific AI skills to the project to make it easier to work with.
* Design artefacts will be in the `design` folder.
*  `design/overview.md` folder.
* The `scratch` directory is designed for running end to end tests and allows working with simulation output.
