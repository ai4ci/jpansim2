# Generate a versioned file name in a subdirectory.

This function generates a function that resolves a file path fragment to
a specific file location, accounting for a versioning strategy involving
the current date. The defaults create a naming strategy that places an
file in the "output" sub-directory of the current project with a
filename suffix including the date.

## Usage

``` r
.outputter(
  directory = .here("output"),
  ...,
  datedFile = !datedSubdirectory,
  datedSubdirectory = FALSE
)
```

## Arguments

- directory:

  the root of the output

- ...:

  not used must be empty

- datedFile:

  do you want the filename to have the date appended?

- datedSubdirectory:

  do you want the files to be placed in a dated subdirectory?

## Value

a function that takes a filename and boolean delete parameter. When
called with a filename component this function will return the absolute
path of a file which is versioned with date. If the file exists and
delete=TRUE it is deleted. (allowing for libraries that refuse to
overwrite existing files)

## Unit tests



    # default appends date to filename:
    out = .outputter(tempdir())
    tmp = out("test.pdf")
    testthat::expect_match(tmp, "^.*/test-[0-9]{4}-[0-9]{2}-[0-9]{2}.pdf$")

    # default appends date to filename:
    out2 = .outputter(tempdir(),datedSubdirectory=TRUE)
    tmp2 = out2("test.pdf")
    testthat::expect_match(tmp2, "^.*/[0-9]{4}-[0-9]{2}-[0-9]{2}/test.pdf$")
