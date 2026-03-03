# Write dataframes as CSV and generate corresponding Java @Import interfaces.

The dataframes must have a column named `id` which is unique. References
to other dataframe rows must be named `<relationship>_<dataframe>` or
just `<dataframe>` where `<dataframe>` is the name of the entity in a
dataframe supplied to `...`, see examples.

## Usage

``` r
write_repository_data(directory, fqn, ...)
```

## Arguments

- directory:

  Path to output directory for CSV and Java files

- fqn:

  Full Java package and class name, e.g. "com.example.model.MyData"

- ...:

  Named dataframes (e.g., country = ..., city = ...) or if a set of
  dataframes, and the variable name will be used as the entity name

## Examples

``` r
library(tibble)
# Example data
country <- tibble::tibble(
  id = c("1", "2", "3"),
  country_name = c("France", "Germany", "Italy")
)

city <- tibble::tibble(
  id = c("101", "102", "103"),
  town_name = c("Paris", "Marseille", "Berlin"),
  country = c("1", "1", "2"),  # matches country$id
  capital = c(TRUE,FALSE,TRUE)
)

person <- tibble::tibble(
  id = c("1001", "1002","1003"),
  first_name = c("Alice", "Bob", "Sasha"),
  given_gender = factor(c("female","male","non binary")),
  source_city = c("101", "103", "102"),  # matches city$id
  target_city = c("102", "101", "102")
)

# Generate Java + CSV
writeRepositoryData(
  directory = "~/tmp/data/",
  fqn = "com.example.model.RepositoryData",
  country,
  city,
  person
)
#> Error in writeRepositoryData(directory = "~/tmp/data/", fqn = "com.example.model.RepositoryData",     country, city, person): could not find function "writeRepositoryData"
```
