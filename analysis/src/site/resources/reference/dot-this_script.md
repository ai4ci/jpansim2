# Get the file path of the current script

Gives you the file input path regardless of whether you are running the
script in rstudio, knitr or on the console.

## Usage

``` r
.this_script()
```

## Value

the file path of the currently executed script or an error if the
command is executed outside of a script.

## Unit tests


    .this_script()
