#' Attempts to apply a mutation to a data frame, handling errors gracefully.
#'
#' @param df A data frame to which the mutation will be applied.
#' @param ... Additional arguments passed to `mutate()`, specifying the mutation(s).
#' @return The mutated data frame if successful; the original data frame if an error occurs.
#' If the result is neither a data frame nor an error, the raw output is returned.
#' @keywords internal
#' @examples
#' df <- tibble::tibble(x = 1:5)
#' try_mutate(df, y = x * 2) # Successfully adds a new
#' try_mutate(df, w = stop()) # Error occurs, returns original df
#' try_mutate(df, v = x + "a") # Error occurs, returns original df
#'
try_mutate = function(df, ...) {
  out = try(
    {
      df %>% dplyr::mutate(...)
    },
    silent = TRUE
  )
  if (is.data.frame(out)) {
    return(out)
  }
  if (class(out) == "try-error") {
    return(df)
  }
  return(out)
}


#' Extracts unique group identifiers from a vector of JPanSim2 URNs.
#'
#' URNs in JPanSim2 will be in the form key:value:key:value:key:value.
#' This function retrieves all the keys.
#'
#' @param urns A character vector of URNs, where each URN is expected
#' to have components separated by colons.
#'
#' @return A character vector of unique group identifiers extracted from the URNs.
#' @keywords internal
#' @examples
#' urns <- c("key1:value1:key2:value2", "keyA:valueA:keyB:valueB")
#' urn_groups(urns)
#'
urn_groups = function(urns) {
  unique(unlist(lapply(stringr::str_split(urns, ":"), function(x) {
    if (length(x) < 2) character() else x[c(TRUE, FALSE)]
  })))
}


#' Splits a vector of JPanSim2 URNs into a tibble of key-value pairs.
#'
#' This function processes URNs in the format key:value:key:value and converts
#' them into a tibble where each row corresponds to a URN and columns represent
#' the extracted keys and their associated values.
#'
#' @param urns A character vector of URNs, where each URN is expected to have
#' components separated by colons.
#' @return A tibble where each column corresponds to a key from the URNs, and
#' rows represent the associated values. If a URN has fewer than two components,
#' it is ignored.
#' @keywords internal
#' @examples
#' urns <- c("key1:value1:key2:value2", "keyA:valueA:keyB:valueB")
#' split_urn(urns)
#'
split_urn = function(urns) {
  dplyr::bind_rows(lapply(stringr::str_split(urns, ":"), \(x) {
    values = x[c(FALSE, TRUE)]
    # deal with default values
    if (length(x) < 2) {
      return(NULL)
    }
    names(values) = x[c(TRUE, FALSE)]
    return(as_tibble(as.list(values)))
  }))
}
