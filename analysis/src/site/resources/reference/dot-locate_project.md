# Find the root of the current project

gets a reverse directory listing and then finds the lowest directory
that contains an indication of there being a project in the directory
this should work where the `here` package does not (particularly when
knitting in CRAN checks for example, or running in a non package
project)

## Usage

``` r
.locate_project(inputFile = NULL)
```

## Arguments

- inputFile:

  a file to check the project root of.

## Value

the file path of the root of the project

## Unit tests


    .locate_project()
