# Find a file in a data directory.

This function generates a function that resolves a file path fragment to
a specific file location in an input directory.

## Usage

``` r
.inputter(directory = .here("input"))
```

## Arguments

- directory:

  the root of the input

## Value

a function that takes a relative path and returns the absolute path of
the input file. The function can take the same arguments as
[`fs::dir_ls`](https://fs.r-lib.org/reference/dir_ls.html), and
particularly useful is `glob` and `type`. The function

## Unit tests


    inp = .inputter(tempdir())
    for (i in 1:10) {
      fs::file_touch(fs::path(tempdir(), sprintf("test_
    }

    testthat::expect_true(fs::file_exists(inp("test_2.txt")))
    testthat::expect_error(inp("test_0.txt"))

    testthat::expect_equal(length(inp(glob="*.txt")),10)
