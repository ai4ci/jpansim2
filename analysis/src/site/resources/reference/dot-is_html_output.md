# Check the current input and output type

The function `is_latex_output()` returns `TRUE` when the output format
is LaTeX; it works for both `.Rnw` and R Markdown documents (for the
latter, the two Pandoc formats `latex` and `beamer` are considered LaTeX
output). The function `is_html_output()` only works for R Markdown
documents and will test for several Pandoc HTML based output formats (by
default, these formats are considered as HTML formats:
`c('markdown', 'epub', 'epub2', 'html', 'html4', 'html5', 'revealjs', 's5', 'slideous', 'slidy', 'gfm')`).

## Usage

``` r
.is_html_output(fmt = pandoc_to(), excludes = NULL)
```

## Arguments

- fmt:

  A character vector of output formats to be checked against. If not
  provided, `is_html_output()` uses `pandoc_to()`, and `pandoc_to()`
  returns the output format name.

- excludes:

  A character vector of output formats that should not be considered as
  HTML format. Options are: markdown, epub, epub2, html, html4, html5,
  revealjs, s5, slideous, slidy, and gfm.

## Details

The function `pandoc_to()` returns the Pandoc output format, and
`pandoc_from()` returns Pandoc input format. `pandoc_to(fmt)` allows to
check the current output format against a set of format names. Both are
to be used with R Markdown documents.

These functions may be useful for conditional output that depends on the
output format. For example, you may write out a LaTeX table in an R
Markdown document when the output format is LaTeX, and an HTML or
Markdown table when the output format is HTML. Use `pandoc_to(fmt)` to
test a more specific Pandoc format.

Internally, the Pandoc output format of the current R Markdown document
is stored in
`knitr::`[`opts_knit`](https://rdrr.io/pkg/knitr/man/opts_knit.html)`$get('rmarkdown.pandoc.to')`,
and the Pandoc input format in
`knitr::`[`opts_knit`](https://rdrr.io/pkg/knitr/man/opts_knit.html)`$get('rmarkdown.pandoc.from')`

## Note

See available Pandoc formats, in [Pandoc's
Manual](https://pandoc.org/MANUAL.html)

## Examples

``` r
# check for output formats type
knitr::is_latex_output()
#> [1] FALSE
knitr::is_html_output()
#> [1] FALSE
knitr::is_html_output(excludes = c("markdown", "epub"))
#> [1] FALSE
# Get current formats
knitr::pandoc_from()
#> [1] "markdown"
knitr::pandoc_to()
#> NULL
# Test if current output format is 'docx'
knitr::pandoc_to("docx")
#> [1] FALSE
```
