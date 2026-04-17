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
  if (is.null(urns)) return(NULL)
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
    names(values) = sprintf("%sDimension",x[c(TRUE, FALSE)])
    return(dplyr::as_tibble(as.list(values)))
  }))
}




#' Get preserved group characteristics
#'
#' @param grp_df a grouped data frame
#' @param distinct defaults to `TRUE`, creates a unique row for every group.
#'
#' @returns a dataframe that contains the groups and other variables that do not 
#'   vary within group
#' @keywords internal
#'
#' @examples
#' grp_df = ggplot2::diamonds %>% 
#'   dplyr::mutate(idx = as.numeric(cut) * as.numeric(color), dup=1) %>% 
#'   dplyr::group_by(cut, color) 
#' grp_df %>% invariants()
invariants = function(grp_df, distinct = TRUE) {
  grps = grp_df %>% dplyr::group_vars()
  cols = grp_df %>% 
    dplyr::summarise(dplyr::across(dplyr::everything(), dplyr::n_distinct),.groups = "keep") %>%
    dplyr::ungroup() %>%
    dplyr::select(dplyr::where(~ is.numeric(.x) && all(.x == 1))) %>% 
    colnames()
  tmp = grp_df %>% dplyr::select(dplyr::all_of(c(grps,cols)))
  if (distinct) tmp = tmp %>% dplyr::distinct()
  return(tmp)
}


#' Get preserved group characteristics that define groups
#'
#' @param grp_df a grouped data frame
#'
#' @returns a dataframe that contains the groups and other variables that do not 
#'   vary within group but do vary between groups.
#' @keywords internal
#'
#' @examples
#' grp_df = ggplot2::diamonds %>% 
#'   dplyr::mutate(idx = as.numeric(cut) * as.numeric(color), dup=1) %>% 
#'   dplyr::group_by(cut, color) 
#' grp_df %>% group_deltas()
group_deltas = function(grp_df) {
  
  grps = grp_df %>% dplyr::group_vars()
  
  .recurse_filt = function(tmp) {
    for (nm in setdiff(colnames(tmp),grps)) {
      if (is.data.frame(tmp[[nm]])) {
        # handle nested data frames
        tmp[[nm]] = .recurse_filt(tmp[[nm]])
      } else {
        if (length(unique(tmp[[nm]]))==1) tmp[[nm]]=NULL
      }
      # TODO: 
    }
    if (ncol(tmp) == 0) return(NULL)
    return(tmp)
  }
  
  return(grp_df %>% invariants() %>% dplyr::ungroup() %>% .recurse_filt())
  
  # grps = tmp %>% dplyr::group_vars()
  # tmp %>% dplyr::ungroup() %>% dplyr::select(dplyr::where(~ length(unique(.x))>1))
}