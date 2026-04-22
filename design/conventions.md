# Coding Style

## Purpose

This note collects the coding conventions, javadoc rules, significant
file locations and CI/test commands that future chat sessions or a human
developer should know when working on `jpansim2`.

Keep this short and practical; the intention is to speed up future
conversations and to remind a developer of repository conventions and
where to find key files.

## Coding style (Java)

- Follow standard Java conventions: camelCase for methods/fields, Pascal
 Case for classes and interfaces, and 4-space indentation.
- Wrap at 80 characters.
- Use of `Immutables` and `MapStruct` annotation processors
- Prefer immutability for small value objects where reasonable.
- Use modern Java idioms (streams, Optional) where ever reasonable.
- Keep methods short: prefer extracting private helper methods when a
 method grows beyond ~60-80 lines.
- Eclipse formatting and style configuration in `code-style-xml` and
`clean-up.xml` files

## Javadoc rules (project-wide)

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

## Coding style (R)

- prefer `tidyverse` idioms
- qualify all namespaces with `::` operators.
- comprehensively document with `Roxygen` all exported function.