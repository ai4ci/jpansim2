# fmt: skip file

# Start pkgtools dependencies ----

# Check dependencies:
if (!requireNamespace("base64enc", quietly=TRUE)) {
  message("Package `base64enc` must be installed.")
}
if (!requireNamespace("dplyr", quietly=TRUE)) {
  message("Package `dplyr` must be installed.")
}
if (!requireNamespace("flextable", quietly=TRUE)) {
  message("Package `flextable` must be installed.")
}
if (!requireNamespace("fs", quietly=TRUE)) {
  message("Package `fs` must be installed.")
}
if (!requireNamespace("ggplot2", quietly=TRUE)) {
  message("Package `ggplot2` must be installed.")
}
if (!requireNamespace("grDevices", quietly=TRUE)) {
  message("Package `grDevices` must be installed.")
}
if (!requireNamespace("htmltools", quietly=TRUE)) {
  message("Package `htmltools` must be installed.")
}
if (!requireNamespace("huxtable", quietly=TRUE)) {
  message("Package `huxtable` must be installed.")
}
if (!requireNamespace("knitr", quietly=TRUE)) {
  message("Package `knitr` must be installed.")
}
if (!requireNamespace("magrittr", quietly=TRUE)) {
  message("Package `magrittr` must be installed.")
}
if (!requireNamespace("officer", quietly=TRUE)) {
  message("Package `officer` must be installed.")
}
if (!requireNamespace("pdftools", quietly=TRUE)) {
  message("Package `pdftools` must be installed.")
}
if (!requireNamespace("purrr", quietly=TRUE)) {
  message("Package `purrr` must be installed.")
}
if (!requireNamespace("ragg", quietly=TRUE)) {
  message("Package `ragg` must be installed.")
}
if (!requireNamespace("rappdirs", quietly=TRUE)) {
  message("Package `rappdirs` must be installed.")
}
if (!requireNamespace("readr", quietly=TRUE)) {
  message("Package `readr` must be installed.")
}
if (!requireNamespace("rlang", quietly=TRUE)) {
  message("Package `rlang` must be installed.")
}
if (!requireNamespace("rmarkdown", quietly=TRUE)) {
  message("Package `rmarkdown` must be installed.")
}
if (!requireNamespace("rstudioapi", quietly=TRUE)) {
  message("Package `rstudioapi` must be installed.")
}
if (!requireNamespace("rsvg", quietly=TRUE)) {
  message("Package `rsvg` must be installed.")
}
if (!requireNamespace("scales", quietly=TRUE)) {
  message("Package `scales` must be installed.")
}
if (!requireNamespace("stats", quietly=TRUE)) {
  message("Package `stats` must be installed.")
}
if (!requireNamespace("stringr", quietly=TRUE)) {
  message("Package `stringr` must be installed.")
}
if (!requireNamespace("svglite", quietly=TRUE)) {
  message("Package `svglite` must be installed.")
}
if (!requireNamespace("systemfonts", quietly=TRUE)) {
  message("Package `systemfonts` must be installed.")
}
if (!requireNamespace("testthat", quietly=TRUE)) {
  message("Package `testthat` must be installed.")
}
if (!requireNamespace("tibble", quietly=TRUE)) {
  message("Package `tibble` must be installed.")
}
if (!requireNamespace("utils", quietly=TRUE)) {
  message("Package `utils` must be installed.")
}

# Deal with stats/dplyr issues:
if (!requireNamespace("conflicted", quietly=TRUE)) {
  message("Package `conflicted` must be installed.")
} else {
  conflicted::conflicts_prefer(
    dplyr::filter(),
    dplyr::lag(),
    .quiet = TRUE
  )
}

# Load standalones:
try(source("R/import-standalone-cache.R"))
try(source("R/import-standalone-directory-utils.R"))
try(source("R/import-standalone-file-output-utils.R"))
try(source("R/import-standalone-ggplot-utils.R"))

# End pkgtools dependencies ----
