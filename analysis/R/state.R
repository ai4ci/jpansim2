state = new.env(parent = emptyenv())

#' Set the JPanSim2 output directory.
#'
#' @param dir The directory to set as the JPanSim2 output directory. If missing
#'   a dialog will be opened to select the directory.
#' @returns the directory to use.
#' @export
#'
#' @examples
#' set_results_directory(tempdir())
#' get_results_directory()
set_results_directory = function(
  dir = NULL
) {
  if (is.null(dir)) {
    dir = tcltk::tk_choose.dir(
      caption = "Pick a JPanSim2 output directory.",
      default = fs::path_home()
    )
  }
  state$results_directory = fs::path_abs(dir)
  return(dir)
}

#' Get the JPanSim2 output directory.
#'
#' @returns the cached directory path. If missing an interactive folder chooser
#' will be displayed.
#' @export
#'
#' @examples
#' set_results_directory(tempdir())
#' get_results_directory()
get_results_dir = function() {
  if (is.null(state$results_directory)) {
    set_results_directory()
  }
  return(state$results_directory)
}
