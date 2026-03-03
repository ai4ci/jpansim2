# Print an SVG file to pdf using chrome

Create a the same size as the SVG file using chrome or chromium

## Usage

``` r
.print_svg_with_chrome(
  svgFile,
  pdfFile = tempfile(fileext = ".pdf"),
  chromeBinary = getOption("ggrrr.chrome", default = .find_chrome()),
  maxWidth = NULL,
  maxHeight = NULL
)
```

## Arguments

- svgFile:

  the input svg file

- pdfFile:

  the output pdf file

- chromeBinary:

  the location of chrome or chromium binary

- maxWidth:

  the maximum width of the output in inches

- maxHeight:

  the maximum height of the output in inches

## Value

the pdf file path

## Unit tests



    tryCatch(.find_chrome(), error = function(e) testthat::skip())

    plot = ggplot2::ggplot(ggplot2::diamonds, ggplot2::aes(x=carat,y=price,color = color))+
       ggplot2::geom_point()+
       ggplot2::annotate("label",x=2,y=10000,label="Hello \u2014 world", family="Kings")+
       ggplot2::labs(tag = "A")+
       ggplot2::xlab("Carat\u2082")+
       ggplot2::ylab("price\u2265")

    res = plot 

    # res_content = readr::read_file_raw(res$svg)
    # testthat::expect_equal(rlang::hash(res_content), "99e7c62601699705bbb13dacf621caca")

    testthat::expect_no_error({
      .print_svg_with_chrome(res$svg)
    })

    # exact binary copy may depend on versions of chrome etc:
