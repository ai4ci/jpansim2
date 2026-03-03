# An opinionated set of defaults for plots

This is a set of styles with a focus on making plots compact, and
minimally fussy, and ensuring fonts are consistent between axes and
labels. It sets default sizes for line widths and point sizes. It also
switched the default png renderer in knitr to `ragg::ragg_png` to allow
for modern font support.

## Usage

``` r
.gg_pedantic(
  lineSize = 0.25,
  fontSize = 8,
  font = "Roboto",
  size = lineSize * 2,
  ...
)
```

## Arguments

- lineSize:

  the default line and shape size in ggplot units

- fontSize:

  the base font size

- font:

  the default font name.

- size:

  the size of points (the default size aesthetic)

- ...:

  passed to
  [`ggplot2::theme`](https://ggplot2.tidyverse.org/reference/theme.html)

## Value

nothing
