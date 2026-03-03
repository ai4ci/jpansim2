#' Read experiment result settings
#'
#' Read and parse the experiment settings stored as JSON in the results directory.
#' If the directory is not provided then it is
#'
#' @param dir Character. Path to the results directory. Defaults to \code{get_results_dir()}.
#' @return A named list containing the parsed experiment settings (result of \code{jsonlite::fromJSON}).
#' @examples
#' \dontrun{
#' settings <- get_experiment_settings()        # use default results directory
#' settings <- get_experiment_settings("out/1") # specify a directory
#' }
#' @export
get_experiment_settings = function(dir = get_results_dir()) {
  configFiles = fs::dir_ls(
    directory,
    recurse = TRUE,
    glob = "*result-settings.json"
  )
  config = dplyr::bind_rows(lapply(configFiles, jsonlite::fromJSON)) %>%
    try_mutate(split_urn(modelName)) %>%
    try_mutate(split_urn(experimentName)) %>%
    dplyr::group_by(dplyr::across(dplyr::any_of(c(
      per_execution,
      urn_groups(.$modelName),
      urn_groups(.$experimentName)
    ))))
  return(config)
}

#' Load all `duckdb` files and CSV files
#'
#' This function loads all the outputs of a simulation set into a single
#' `duckdb` container and provides access to it as `dplyr` tables. It combines
#' outputs from multiple SLURM nodes if needed.
#'
#' @param directory the directory where simulation results are found. This may
#'   be a flat directory from simulations run on a single node, or a set of
#'   directories with the output of multiple nodes. Within the output
#'   directory/ies the individual `duckdb` and `csv` files are all consistently
#'   named and these names will be in the output.
#'
#' @returns A named list of lazy `tbl` dataframes backed by `duckdb`. These can
#'   be queried with `dplyr` but natively support will be slightly limited by
#'   the `duckdb` backed. The list will have a `con` attribute holding the
#'   `duckdb` connection.
#'
#'   If full `dplyr` or `purrr` is needed then a `dplyr::collect()` call will
#'   copy the individual tables into memory.
#' 
#' @examples
#' tmp = load_duckdb("/home/vp22681/Data/ai4ci/lockdown-compliance")
#'
load_duckdb = function(
  directory = get_results_dir()
) {
  duckfiles = fs::dir_ls(directory, recurse = TRUE, glob="*.duckdb")
  csvfiles = fs::dir_ls(directory, recurse = TRUE, glob="*.csv")
  
  # A DuckDb in memory database:
  con = duckdb::dbConnect(duckdb::duckdb())
  # DBI::dbDisconnect(con)
  
  # Load the CSVs (requires parsing)
  csvs = unique(unname(stringr::str_extract(csvfiles, ".*/(.+)\\.csv$", group = 1)))
  # CSV files can be loaded using glob syntax which makes this super easy:
  tables = lapply(csvs, function(csv) {
    DBI::dbSendQuery(con, sprintf("CREATE VIEW '%1$s' AS SELECT * FROM '%2$s/**/%1$s.csv'",csv, directory))
    return(dplyr::tbl(con,csv))
  })
  names(tables) = csvs
  
  # Load the duckdbs (instant)
  ducks = unname(stringr::str_extract(duckfiles, ".*/(.+)\\.duckdb$", group = 1))
  # Name the file paths with the type:
  names(duckfiles) = ducks
  # the unique types:
  ducks = unique(ducks)
  # load each file with duckdb and join dataframes.
  tables2 = lapply(seq_along(ducks), function(i) {
    # For each type of duck db - e.g. "demog" "linelist" etc:
    ducknm = ducks[i]
    # There may be multiple files one from each simulation node
    ducknmpaths = duckfiles[ducknm]
    schematbls = lapply(seq_along(ducknmpaths), function(j) {
      # The path of one duckdb file:
      tmppath = ducknmpaths[j]
      # Each table is attached into a separate schema based on this arbitrary (?) index.
      tmpschema = sprintf("%s_%d",ducknm, j)
      # The table is already named in the duckdb file we are attaching and it 
      # is the same as the file name (by design in the simulation engine)
      DBI::dbSendQuery(con, sprintf("ATTACH '%s' AS %s", tmppath, tmpschema))
      return(sprintf("SELECT * FROM %s.%s", tmpschema, ducknm))
    })
    # schematbls is the list of select statements one for each schema.
    # We want the union of this as a view:
    viewquery = sprintf("CREATE VIEW %s AS %s", ducknm, paste0(schematbls, collapse = " UNION "))
    DBI::dbSendQuery(con, viewquery)
    return(dplyr::tbl(con,ducknm))
  })
  names(tables2) = ducks
  
  cols = tmp$ip %>% dplyr::select(experimentName) %>% dplyr::distinct() %>%
    dplyr::pull(experimentName)
  
  tables2$ip %>% 
    dplyr::mutate(!!cols[1] := dplyr::sql("regexp_split_to_array(experimentName,':')[2]")) %>% dplyr::glimpse()
  
  # The CSV views and the duckdb views together:
  return(structure(
    c(tables,tables2),
    con = con)
  )
}

load_csvs = function(
  directory,
  type = c(
    "summary",
    "contact-counts",
    "ip",
    "behaviours",
    "final-state",
    "test-positivity",
    "debug"
  )
) {
  type = match.arg(type)
  files = fs::dir_ls(directory, recurse = TRUE)
  files = files[stringr::str_ends(files, paste0(type, ".csv"))]
  listData = lapply(files, readr::read_csv, show_col_types = FALSE)
  listData = purrr::keep(listData, ~ nrow(.x) > 0)
  loaded = .cached(
    dplyr::bind_rows(listData) %>%
      try_mutate(split_urn(modelName)) %>%
      try_mutate(split_urn(experimentName)) %>%
      group_by(across(any_of(c(
        per_execution,
        urn_groups(.$modelName),
        urn_groups(.$experimentName)
      )))),
    files,
    fs::file_info(files)$modification_time
  )
  return(loaded)
}
