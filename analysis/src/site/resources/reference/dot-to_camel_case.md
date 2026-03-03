# Convert snake_case strings to camelCase or PascalCase

Convert snake_case strings to camelCase or PascalCase

## Usage

``` r
.to_camel_case(x, capitalize_first = TRUE)
```

## Arguments

- x:

  A character vector with snake_case names.

- capitalize_first:

  Logical; if TRUE, returns PascalCase (e.g., "MyVariable"). If FALSE
  (default), returns camelCase (e.g., "myVariable").

## Value

A character vector with converted names.

## Examples

``` r
.to_camel_case("hello_world")
#> Error in .to_camel_case("hello_world"): could not find function ".to_camel_case"
.to_camel_case("very_long_variable_name", capitalize_first = TRUE)
#> Error in .to_camel_case("very_long_variable_name", capitalize_first = TRUE): could not find function ".to_camel_case"
.to_camel_case("alreadyCamelCase", capitalize_first = TRUE)
#> Error in .to_camel_case("alreadyCamelCase", capitalize_first = TRUE): could not find function ".to_camel_case"
.to_camel_case(c("hello_world","very_long_variable_name"), capitalize_first = FALSE)
#> Error in .to_camel_case(c("hello_world", "very_long_variable_name"), capitalize_first = FALSE): could not find function ".to_camel_case"
```
