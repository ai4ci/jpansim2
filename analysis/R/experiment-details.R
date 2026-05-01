#' @param data a dataframe from the simulation output; typically derived from 
#'   one of the output of the `load_duckdb()` or `get_tables()` functions.
#' @param dir the directory where simulation results are found. This may
#'   be a flat directory from simulations run on a single node, or a set of
#'   directories with the output of multiple nodes. Within the output
#'   directory/ies the individual `duckdb` and `csv` files are all consistently
#'   named by the simulation.
#' @param tables the output of the  `load_duckdb()` or `get_tables()` function
#'
#' @name common-jpansim
#' @keywords internal


#' Read experiment result settings
#'
#' Read and parse the experiment settings stored as JSON in the results directory.
#' If the directory is not provided then it is
#'
#' @inheritParams common-jpansim
#' @return A named list containing the parsed experiment settings (result of \code{jsonlite::fromJSON}).
#' @examples
#' \dontrun{
#' settings = get_experiment_settings()        # use default results directory
#' settings = get_experiment_settings("/home/vp22681/Data/ai4ci/0.3.2/behaviour-comparison") # specify a directory
#' }
#' @export
get_experiment_settings = function(dir = get_results_dir()) {
  configFiles = fs::dir_ls(
    dir,
    recurse = TRUE,
    glob = "*result-settings.json"
  )
  # browser()
  config = dplyr::bind_rows(lapply(configFiles, jsonlite::fromJSON)) %>%
    try_mutate(split_urn(modelName)) %>%
    try_mutate(split_urn(experimentName))
  # %>%
  #   dplyr::group_by(dplyr::across(dplyr::any_of(c(
  #     urn_groups(.$modelName),
  #     urn_groups(.$experimentName)
  #   ))))
  return(config)
}

#' Load all `duckdb` files and CSV files
#'
#' This function loads all the outputs of a simulation set into a single
#' `duckdb` container and provides access to it as `dplyr` tables. It combines
#' outputs from multiple SLURM nodes if needed. It augments the raw output data
#' with the names of the facets in each simulation.
#'
#' @inheritParams common-jpansim
#'
#' @returns A named list:
#'   - `dimensions`: a list which details how many different simulations were
#'   run and the breakdown of whether these were replications or comparisons.
#'   - `experiments`: a lazy `tbl` dataframe backed by `duckdb` which links the
#'   experiment name with the combination of changes made to the default
#'   configuration for that experiment.
#'   - `models`: a table which links the
#'   experiment name with the combination of changes made to the default
#'   configuration for that model.
#'   - `parameters`: a  lazy `tbl` dataframes backed by `duckdb` which links
#'   each simulation run to the full parametrisation (of both the model setup
#'   and execution). This has all the configuration used for that model run.
#'   - remaining entries are lazy `tbl` dataframes backed by `duckdb`. These can
#'   be queried with `dplyr` but natively support will be slightly limited by
#'   the `duckdb` backed.
#'
#'   The return value will have a `con` attribute holding the `duckdb`
#'   connection.
#'
#'   When using the `comparisons` or `parameters` tables, native `dplyr` or is
#'   needed for the list columns. The `duckdb` tables will need to be copied
#'   into the R session with a `dplyr::collect()` call.
#'
#' @examples
#' tables = load_duckdb(here("../scratch/examples/behaviour-comparison"))
#' 
load_duckdb = function(
  dir = get_results_dir()
) {
  duckfiles = fs::dir_ls(dir, recurse = TRUE, glob="*.duckdb")
  csvfiles = fs::dir_ls(dir, recurse = TRUE, glob="*.csv")
  
  # A DuckDb in memory database:
  con = duckdb::dbConnect(duckdb::duckdb())
  # DBI::dbDisconnect(con)
  
  # Load the CSVs (requires parsing)
  csvs = unique(unname(stringr::str_extract(csvfiles, ".*/(.+)\\.csv$", group = 1)))
  # CSV files can be loaded using glob syntax which makes this super easy:
  tables = lapply(csvs, function(csv) {
    DBI::dbSendQuery(con, sprintf("
      CREATE VIEW '%1$s' AS 
        SELECT * FROM read_csv(
          ['%2$s/**/%1$s.csv'], 
          sample_size = 1000, 
          types = {
            'time':'INTEGER',
            'count':'INTEGER',
            'modelReplica':'INTEGER',
            'experimentReplica':'INTEGER'
          }, 
          ignore_errors = true
        )",csv, dir))
    # N.B. lots of other columns in summary such as infectedCount, hospitalisedCount
    return(dplyr::tbl(con,csv))
  })
  names(tables) = stringr::str_replace(csvs,"[^a-z]","_")
  
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
  names(tables2) = stringr::str_replace(ducks,"[^a-z]","_")
  
  # browser()
  cfg = jsonlite::read_json(fs::path(dir,"config.json"))
  
  # N.b. this can be wrong because in my wisdom it is possible to modify
  # a model as well as supply multiple baseline models. If the former
  # approach is taken then there are model urns like `model-name:modification-name`
  # otherwise it will just be `model-name`
  # count = list(
  #   models = max(c(length(cfg$setupConfig),1)),
  #   modelReplicas = cfg$setupReplications,
  #   comparisons = length(cfg$facets),
  #   comparisonNames = sprintf("%sDimension",sapply(cfg$facets, `[[`, "name")),
  #   experiments = prod(sapply(cfg$facets, function(f) {max(c(1,length(f$modifications)))})),
  #   experimentReplicas = cfg$executionReplications
  # )
  # 
  # count$experimentRuns = count$experiments * count$experimentReplicas
  # count$modelRuns =  count$models * count$modelReplicas
  # count$simulations = count$modelRuns * count$experimentRuns
  
  # This gives us the column names that are likely to be useful in facetting:
  # count$names = c((if(count$models > 1) "modelName" else NULL), count$comparisonNames)
  
  # facets = lapply(cfg$facets, function(f) {
  #   nm = f$name
  #   dplyr::bind_rows(lapply(f$modifications, function(m) {
  #     mod = m
  #     mod$name = NULL
  #     dplyr::tibble(
  #       urn_part = sprintf("%s:%s", nm, m$name),
  #       !!sprintf("%sDimension",nm) := m$name,
  #       mod_part = list(mod),
  #     )
  #   }))
  # })
  
  # TODO: need a similar output for cfg$setupConfig
  # browser()

  # Generate URN from config:
  # This is replicating the logic to generate a URN from the experiment and its
  # facets that occurs in the simulation. If that logic changes this will break.
  
  # expt = Reduce(f=function(l,r) {
  #   tmp = l %>%
  #            dplyr::cross_join(r) %>%
  #            dplyr::mutate(
  #              experimentName = ifelse(experimentName != "", sprintf("%s:%s",experimentName,urn_part), urn_part),
  #              modification = purrr::map2(modification, mod_part,utils::modifyList)
  #            ) %>%
  #            dplyr::select(-urn_part,-mod_part)
  #   return(tmp)
  # },
  # x = facets,
  # init = dplyr::tibble(experimentName="", modification = list(list())))
  
  # using the first table for experiment names but other ones could be possible
  # cols = tables[[1]] %>% 
  #   dplyr::select(experimentName) %>% 
  #   dplyr::distinct() %>%
  #   dplyr::pull(experimentName)
  # expt = cols %>% split_urn() %>% 
  #   dplyr::mutate(dplyr::across(dplyr::everything(), forcats::as_factor)) %>%
  #   # rename the facet columns
  #   dplyr::rename_with(~ sprintf("%sFacet",.x)) %>%
  #   dplyr::mutate(experimentName = cols) %>%
  #   dplyr::as_tibble()
  
  full_params = get_experiment_settings(dir)
  # cut down version including names only so that it will work in duckdb
  # otherwise nested dataframes cause issues.
  
  expt = full_params %>% 
    tidyr::unnest(dplyr::everything(),names_sep = ".",keep_empty = TRUE) %>%
    dplyr::group_by(modelName,experimentName)%>% 
    group_deltas()
  
    
  mdl = full_params %>% 
    tidyr::unnest(dplyr::everything(),names_sep = ".",keep_empty = TRUE) %>%
    dplyr::group_by(modelName) %>% 
    group_deltas()
  
  count = list(
    models = dplyr::n_distinct(full_params$modelName),
    modelReplicas = dplyr::n_distinct(full_params$modelReplica),
    comparisonNames = colnames(full_params %>% dplyr::select(dplyr::ends_with("Dimension"))),
    experiments = dplyr::n_distinct(full_params$experimentName),
    experimentReplicas = dplyr::n_distinct(full_params$experimentReplica),
    simulations = nrow(full_params)
  )
  
  count$comparisons = length(count$comparisonNames)
  count$experimentRuns = count$experiments * count$experimentReplicas
  count$modelRuns =  count$models * count$modelReplicas
  
  # Add the facet / comparison details into the main data
  # aug = lapply(c(
  #   tables,
  #   tables2), function(t) {
  #     join_cols = intersect(colnames(t), c("experimentName", "modelName","modelReplica","experimentReplica"))
  #     if (length(join_cols) > 0) {
  #       tmp = full_params %>% 
  #         dplyr::group_by(dplyr::across(dplyr::all_of(join_cols))) %>% 
  #         group_deltas() %>%
  #         tidyr::unnest(dplyr::everything(),names_sep = "_",keep_empty = TRUE)
  #       t %>% dplyr::left_join(tmp, by=join_cols, copy = TRUE)
  #     } else {
  #       t
  #     }
  #   })
  
  duckdb::duckdb_register(con, name = "experiments", expt)
  duckdb::duckdb_register(con, name = "models", mdl)
  expt = dplyr::tbl(con,"experiments")
  mdl = dplyr::tbl(con,"models")
  
  
  # The actual full parameters of evey replication are here
  
  # tmp = expt[[1]]
  # class(tmp)
  
  # The CSV views and the duckdb views together:
  return(structure(
    c(
      c(
        tables,
        tables2),
      list(
        "dimensions" = count,
        "models" = mdl,
        "experiments" = expt,
        "parameters" = full_params,
        "config" = cfg
      )
    ),
    con = con)
  )
}

#' Augment results with definitional parameters.
#' 
#' Add in definitional parameters for each combination of the model and 
#' experiment run that are contained in the output. Only the parameters that
#' are different between groups are added here.
#' 
#' @inheritParams common-jpansim
#' 
#' @returns a dataframe based on `data` with additional columns containing 
#'   the group definitional parameters if they exist.
#' @export
#'
#' @examples
#' set_results_dir(here("../scratch/examples/lockdown-compliance"))
#' get_table("summary") %>% augment_with_parameters() 
augment_with_parameters = function(data, tables = get_tables()) {
  
  t = data
  join_cols = intersect(colnames(t), c("experimentName", "modelName"))
  
  if (length(join_cols) == 2) {
    join_tbl = tables$experiments
    t = t %>% dplyr::left_join(join_tbl, by=join_cols, copy = TRUE)
  } else if (join_cols == "modelName") {
    join_tbl = tables$models
    t = t %>% dplyr::left_join(join_tbl, by=join_cols, copy = TRUE)
  } else if (join_cols > 0) {
    stop("Unexpected joins in augment: ",join_cols)
  }
  
  t = t
  
  return(t)
  
}



#' Group the output of the results by simulation run and day
#'
#' @inheritParams common-jpansim
#' @param ... additional columns in `data` to group by over and above 
#'   the simulation identifiers
#' @param replicas treat each replica of a simulation separately? defaults to `FALSE`
#'
#' @returns a grouped dataframe using default columns where each group is an 
#'   single day in one simulation run (`replicas=TRUE`) or a single day in all
#'   simulation replicas (`replicas=TRUE`)
#' @export
group_by_simulation_day = function(data, ..., replicas = FALSE) {
  s = rlang::ensyms(...)
  s = unname(unlist(sapply(s, rlang::as_label)))
  v = c("time","modelName","experimentName")
  if (replicas) {
    v = c(v,"modelReplica","experimentReplica")
  }
  v = unique(c(v,s))
  data = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v)))
  return(data)
}

#' Group the output of the results by simulation run
#'
#' @inheritParams common-jpansim
#' @param ... additional columns in `data` to group by over and above 
#'   the simulation identifiers
#' @param replicas treat each replica of a simulation separately? defaults to `FALSE`
#'
#' @returns a grouped dataframe using default columns where each group is an 
#'   single day in one simulation run (`replicas=TRUE`) or a single day in all
#'   simulation replicas (`replicas=TRUE`)
#' @export
group_by_simulation = function(data, ..., replicas = FALSE) {
  s = rlang::ensyms(...)
  s = unname(unlist(sapply(s, rlang::as_label)))
  v = c("modelName","experimentName")
  if (replicas) {
    v = c(v,"modelReplica","experimentReplica")
  }
  v = unique(c(v,s))
  data = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v)))
  return(data)
}