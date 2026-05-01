state = new.env(parent = emptyenv())

## RESULT DIRECTORY ----

#' @inherit .here
#' @export
#' @examples
#' print(here())
here = .here

#' Set the JPanSim2 output directory.
#'
#' @param dir The directory to set as the JPanSim2 output directory. If missing
#'   a dialog will be opened to select the directory.
#' @returns the directory to use.
#' @export
#'
#' @examples
#' set_results_dir(tempdir())
#' get_results_dir()
set_results_dir = function(
  dir = NULL
) {
  if (is.null(dir)) {
    dir = tcltk::tk_choose.dir(
      caption = "Pick a JPanSim2 output directory.",
      default = fs::path_home()
    )
    if (is.na(dir)) stop("No directory selected.")
  }
  if (is.null(state$results_directory) || dir != state$results_directory) {
    state$results_directory = fs::path_abs(dir)
    state$tables = NULL
  }
  return(state$results_directory)
}

#' Get the JPanSim2 output directory.
#'
#' @returns the cached directory path. If missing an interactive folder chooser
#' will be displayed.
#' @export
#'
#' @examples
#' set_results_dir(tempdir())
#' get_results_dir()
get_results_dir = function() {
  if (is.null(state$results_directory)) {
    set_results_dir()
  }
  return(state$results_directory)
}

#' Temporarily set results directory and evaluate an expression
#'
#' @param dir the results directory
#' @param expr the expression to evaluate
#'
#' @returns the result of the expression
#' @export
#'
#' @examples
#' with_results_dir(tempdir(), cat(get_results_dir()))
with_results_dir =function(
    dir,
    expr
) {
  
  old = state$results_directory
  oldtbls = state$tables
  on.exit(expr = {
    state$results_directory = old
    state$tables = oldtbls
    returnValue()
  },add = TRUE)
  tmp = set_results_dir(dir)
  eval(expr,envir = rlang::caller_env())
}

## TABLES ----

#' Set the currently loaded simulation output.
#'
#' @param dir The directory to set as the JPanSim2 output directory. If missing
#'   (and not already set) a dialog will be opened to select the directory.
#' @inheritSection load_duckdb returns
#' @export
#'
#' @examples
#' set_results_dir(tempdir())
#' get_tables()
set_tables = function(
    dir = get_results_dir()
) {
  set_results_dir(dir)
  state$tables = load_duckdb(dir)
  return(state$tables)
}

#' Get the JPanSim2 output directory.
#'
#' @inheritSection load_duckdb returns
#' @export
#'
#' @examples
#' set_results_dir(tempdir())
#' get_tables()
get_tables = function() {
  if (is.null(state$tables)) {
    set_tables()
  }
  return(state$tables)
}

#' Get the JPanSim2 output directory.
#'
#' @returns a single duckdb table
#' @export
#'
#' @examples
#' set_results_dir(tempdir())
#' get_tables()
get_table = function(name) {
  tables = get_tables()
  if (!name %in% names(tables)) stop("No such table: ",name)
  return(tables[[name]])
}

## DEFAULTS ----

set_policy_colours = function(named_colours = NULL) {
  if (is.null(named_colours)) {
    # defaults:
    named_colours = c("lockdown"="#FF8080")
  }
  state$policy_colours = named_colours
  return(state$policy_colours)
}

#' Get the colours and names of policy state highlights.
#'
#' @returns a names list
#' @export
get_policy_colours = function() {
  if (is.null(state$policy_colours)) {
    set_policy_colours()
  }
  return(state$policy_colours)
}



