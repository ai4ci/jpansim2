# Check if any fonts listed are missing

Check if any fonts listed are missing

## Usage

``` r
.gg_fonts_missing(family)
```

## Arguments

- family:

  the font family

## Value

`TRUE` if missing fonts detected

## Examples

``` r
try({
  .gg_fonts_missing("Arial")
  .gg_fonts_missing(c("Roboto","Kings","ASDASDAS"))
})
#> Error in .gg_fonts_missing("Arial") : 
#>   could not find function ".gg_fonts_missing"
```
