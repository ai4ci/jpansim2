# Save a plot to multiple formats

Saves a ggplot object to disk at a set physical size. Allows specific
maximum dimensions with an optional target aspect ratio to fit into
specific configurations for publication. e.g. a half page plot or a
third of a 2 column page. Allows output in pdf for journal publication
or png for inclusion in documents, and makes sure that the outputs are
near identical.

## Usage

``` r
.gg_save_as(
  plot,
  filename = tempfile(),
  size = std_size$half,
  maxWidth = size$width,
  maxHeight = size$height,
  aspectRatio = maxWidth/maxHeight,
  formats = getOption("ggrrr.formats", default = c("svg", "png", "pdf")),
  webfontFinder = ~return(list()),
  ...
)
```

## Arguments

- plot:

  a ggplot

- filename:

  base of target filename (excluding extension).

- size:

  a standard size see `std_size`

- maxWidth:

  maximum width in inches

- maxHeight:

  maximum height in inches

- aspectRatio:

  defaults to maxWidth/maxHeight

- formats:

  some of svg, png, pdf, Rdata, eps, ...

- webfontFinder:

  a function that takes a plot and returns a properly formatted css
  specification for webfonts in the plot. This is for internal use and
  does not need to be changed.

- ...:

  Named arguments passed on to
  [`svglite::svglite`](https://svglite.r-lib.org/reference/svglite.html)

  `pointsize`

  :   Default point size.

  `standalone`

  :   Produce a standalone svg file? If `FALSE`, omits xml header and
      default namespace.

  `id`

  :   A character vector of ids to assign to the generated SVG's. If
      creating more SVG files than supplied ids the exceeding SVG's will
      not have an id tag and a warning will be thrown.

  `fix_text_size`

  :   Should the width of strings be fixed so that it doesn't change
      between svg renderers depending on their font rendering? Defaults
      to `TRUE`. If `TRUE` each string will have the `textLength` CSS
      property set to the width calculated by systemfonts and
      `lengthAdjust='spacingAndGlyphs'`. Setting this to `FALSE` can be
      beneficial for heavy post-processing that may change content or
      style of strings, but may lead to inconsistencies between strings
      and graphic elements that depend on the dimensions of the string
      (e.g. label borders and background).

  `scaling`

  :   A scaling factor to apply to the rendered line width and text
      size. Useful for getting the right sizing at the dimension that
      you need.

  `always_valid`

  :   Should the svgfile be a valid svg file while it is being written
      to? Setting this to `TRUE` will incur a considerable performance
      hit (\>50% additional rendering time) so this should only be set
      to `TRUE` if the file is being parsed while it is still being
      written to.

  `file`

  :   Identical to `filename`. Provided for backward compatibility.

## Value

the output is an sensible default object that can be displayed given the
context it is called in, for example if knitting an RMarkdown document a
link to the png file for embedding, if latex a link to the pdf file.

## Details

For maximum cross platform reproducibility we are using the combination
of `systemfonts` for font management, `svglite` to render the canonical
output `rsvg` to convert that to pdf, and `ragg` for bitmap formats. In
some situations `rsvg` fails in which case we fall back to rendering in
a headless chrome instance. This rather complicated pipeline ensures
modern webfont support, and editable SVG or PDF.

## Unit tests


    .gg_pedantic(fontSize = 6)
    p = ggplot2::ggplot(mtcars, ggplot2::aes(mpg, wt, colour=as.factor(cyl))) +
      ggplot2::geom_point()
    # p 
    p 

    plot = ggplot2::ggplot(ggplot2::diamonds, ggplot2::aes(x=carat,y=price,color = color))+
      ggplot2::geom_point()+
      ggplot2::annotate("label",x=2,y=10000,label="Hello \u2014 world", family="Kings")+
      ggplot2::labs(tag = "A")+
      ggplot2::xlab("Carat\u2082")+
      ggplot2::ylab("price\u2265")

    # plot 
    res = plot 
    as.character(res)
    res
