# Find Google Chrome or Chromium in the system

On Windows, this function tries to find Chrome from the registry. On
macOS, it returns a hard-coded path of Chrome under `/Applications`. On
Linux, it searches for `chromium-browser` and `google-chrome` from the
system's `PATH` variable.

## Usage

``` r
.find_chrome()
```

## Value

A character string with the path of chrome or an error.

## Details

This function is borrowed from `pagedown` here to avoid the dependency:
https://github.com/rstudio/pagedown/blob/main/R/chrome.R which is MIT
licensed. We note that there is no check made on MacOS that the path is
correct, and since it may be installed elsewhere this is a potential
point of failure. Must assume that calls to chrome may fail.

## Unit tests


    # no test possible: this will be skipped
    try(.find_chrome())
