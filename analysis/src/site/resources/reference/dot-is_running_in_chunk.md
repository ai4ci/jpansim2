# Check if the current code is being executed in a document chunk in an RStudio session or when being knitted.

Check if the current code is being executed in a document chunk in an
RStudio session or when being knitted.

## Usage

``` r
.is_running_in_chunk()
```

## Value

TRUE is being knitted OR running in chunk in RStudio FALSE if not
interactive or interactive but in console in RStudio
