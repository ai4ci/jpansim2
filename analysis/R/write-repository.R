#' Write dataframes as CSV and generate corresponding Java @Import interfaces.
#'
#' The dataframes must have a column named `id` which is unique. References to
#' other dataframe rows must be named `<relationship>_<dataframe>` or just
#' `<dataframe>` where `<dataframe>` is the name of the entity in a dataframe
#' supplied to `...`, see examples.
#'
#' @param directory Path to output directory for CSV and Java files
#' @param fqn Full Java package and class name, e.g. "com.example.model.MyData"
#' @param ... Named dataframes (e.g., country = ..., city = ...) or
#'   if a set of dataframes, and the variable name will be used as the entity
#'   name
#'
#' @export
#' @examples
#' library(tibble)
#' # Example data
#' country <- tibble::tibble(
#'   id = c("1", "2", "3"),
#'   country_name = c("France", "Germany", "Italy")
#' )
#'
#' city <- tibble::tibble(
#'   id = c("101", "102", "103"),
#'   town_name = c("Paris", "Marseille", "Berlin"),
#'   country = c("1", "1", "2"),  # matches country$id
#'   capital = c(TRUE,FALSE,TRUE)
#' )
#'
#' person <- tibble::tibble(
#'   id = c("1001", "1002","1003"),
#'   first_name = c("Alice", "Bob", "Sasha"),
#'   given_gender = factor(c("female","male","non binary")),
#'   source_city = c("101", "103", "102"),  # matches city$id
#'   target_city = c("102", "101", "102")
#' )
#'
#' # Generate Java + CSV
#' writeRepositoryData(
#'   directory = "~/tmp/data/",
#'   fqn = "com.example.model.RepositoryData",
#'   country,
#'   city,
#'   person
#' )
write_repository_data <- function(directory, fqn, ...) {
  # Capture inputs
  dots <- list(...)
  if (length(dots) == 0) {
    stop("No dataframes provided.")
  }

  # Get names (from dots or call)
  tmp <- match.call(expand.dots = FALSE)
  labels <- sapply(tmp[[4]], rlang::as_label)
  names <- ifelse(rlang::names2(dots) != "", rlang::names2(dots), labels)

  # dots = list(city,country,person)
  # names = c("city","country","person")

  # Validate
  dots = purrr::map2(
    dots,
    names,
    function(.x, .y) {
      if (!is.data.frame(.x)) {
        stop("Input '", .y, "' is not a data.frame.")
      }
      if (!"id" %in% colnames(.x)) {
        stop("Dataframe '", .y, "' is missing 'id' column.")
      }
      if (!is.character(.x$id)) {
        .x$id <- as.character(.x$id)
      }
      return(.x)
    }
  )

  # Create initial dataframe
  df_meta <- tibble::tibble(
    name_r = names,
    df = dots
  ) %>%
    dplyr::mutate(
      name_java = .to_camel_case(name_r),
      csv_file = paste0(name_java, ".csv"),
      # Normalize colnames to camelCase
      df = map(
        df,
        ~ {
          colnames(.) <- .to_camel_case(colnames(.), capitalize_first = FALSE)
          .
        }
      )
    )

  # ——————————————————————————————
  # 0. Determine save locations:
  # ——————————————————————————————

  path = strsplit(fqn, ".", fixed = TRUE)[[1]]
  class_name = tail(path, 1)
  source_directory = c(directory, "src", "main", "java", head(path, -1))
  source_directory = do.call(fs::path, as.list(source_directory))
  fs::dir_create(source_directory, recurse = TRUE)
  data_directory = c(directory, "src", "main", "resources", class_name)
  data_directory = do.call(fs::path, as.list(data_directory))
  fs::dir_create(data_directory, recurse = TRUE)

  # ——————————————————————————————
  # 1. Extract factor columns → enum definitions
  # ——————————————————————————————
  enum_info <- df_meta %>%
    dplyr::transmute(
      factor_cols = map(
        df,
        function(d) {
          is_fac <- sapply(d, is.factor)
          tibble::tibble(
            col_name = colnames(d)[is_fac],
            levels = map(colnames(d)[is_fac], ~ levels(d[[.x]]))
          )
        }
      )
    ) %>%
    tidyr::unnest(factor_cols) %>%
    tidyr::unnest(levels) %>%
    dplyr::mutate(
      enum_name = .to_camel_case(col_name, capitalize_first = TRUE),
      java_levels = sprintf('@Level("%s") %s', levels, .to_snake_case(levels))
    ) %>%
    dplyr::group_by(enum_name) %>%
    dplyr::summarise(
      enum_body = paste0(java_levels, collapse = c(",\n\t\t\t\t"))
    ) %>%
    dplyr::group_by(enum_name) %>%
    dplyr::mutate(
      java_enum = paste0(
        c(
          "",
          sprintf("public static enum %s implements Factor {", enum_name),
          sprintf("\t\t%s", enum_body),
          "}"
        ),
        collapse = "\n\t\t"
      )
    )

  # Collect enums as lines
  enum_lines <- if (nrow(enum_info) > 0) {
    # strip out duplicate definitions.
    unique(enum_info$java_enum)
  } else {
    character(0)
  }

  # ——————————————————————————————
  # 2. Write CSVs
  # ——————————————————————————————
  # No need to transform factor levels — write as-is

  df_meta <- df_meta %>%
    dplyr::mutate(
      path = fs::path(data_directory, csv_file),
      write_result = map2(df, path, ~ readr::write_csv(.x, .y))
    )

  # ——————————————————————————————
  # 3. Build Java interface definitions
  # ——————————————————————————————

  interfaces <- df_meta %>%
    dplyr::mutate(
      cols = map(
        df,
        ~ tibble::tibble(
          col = colnames(.x),
          java_type = sapply(colnames(.x), \(s) {
            .r_type_to_java_type(.x[[s]], s, df_meta$name_java)
          }),
          is_enum = sapply(.x, is.factor),
          nullable = sapply(.x, anyNA)
        )
      ),
      .keep = "unused"
    ) %>%
    tidyr::unnest(cols) %>%
    dplyr::mutate(
      method_line = sprintf(
        "%s%s%s %s%s();",
        ifelse(nullable, "@Nullable ", ""),
        ifelse(col == "id", "@Import.Id ", ""), #id annotation
        java_type,
        ifelse(java_type == "boolean", "is", "get"), # getter prefix
        .upper1(col)
      )
    ) %>%
    dplyr::group_by(name_java, csv_file) %>%
    dplyr::summarise(
      method_body = paste0(method_line, collapse = "\n\t\t\t\t")
    ) %>%
    dplyr::mutate(
      iface_lines = paste0(
        c(
          "@Value.Immutable",
          sprintf("@Import(\"%s\")", csv_file),
          sprintf(
            "public static interface %s extends Indexed<%s> {",
            name_java,
            name_java
          ),
          sprintf("\t\t%s", method_body),
          "}"
        ),
        collapse = "\n\t\t"
      )
    )

  all_iface_lines <- paste0(
    c("", unlist(interfaces$iface_lines)),
    collapse = "\n\n\t\t"
  )

  repo_collection = sprintf(
    "\n\n\t\tpublic static final Class<?>[] TYPES = new Class<?>[] {\n\t\t%s};",
    paste0(
      sprintf("\t\t%s.class", interfaces$name_java),
      collapse = ",\n\t\t"
    )
  )

  # cat(all_iface_lines)

  # ——————————————————————————————
  # 4. Write Java file
  # ——————————————————————————————
  package_parts <- strsplit(fqn, "\\.")[[1]]
  class_name <- utils::tail(package_parts, 1)
  package_name <- paste(utils::head(package_parts, -1), collapse = ".")

  java_file <- file.path(source_directory, paste0(class_name, ".java"))

  java_lines <- c(
    if (nzchar(package_name)) paste0("package ", package_name, ";"),
    "",
    "import org.immutables.value.Value;",
    "import io.github.ai4ci.Import;",
    "import io.github.ai4ci.util.Factor;", # Add Factor import
    # "import io.github.ai4ci.util.Factor.Level;",
    "import io.github.ai4ci.util.Repository;",
    "import io.github.ai4ci.util.Repository.Indexed;",
    "import javax.annotation.Nullable;",
    "import java.nio.file.Paths;",
    "",
    "@Value.Style(",
    "\t\tdeepImmutablesDetection = false,",
    "\t\tpassAnnotations = {Import.class, Import.Id.class},",
    "\t\tget = {\"is*\", \"get*\"}", # allow for boolean is
    ")",
    sprintf("public class %s {", class_name),
    "",
    "\t\tpublic static Repository load() {",
    "\t\t\t\ttry {",
    sprintf(
      "\t\t\t\t\t\tvar path = Paths.get(%s.class.getClassLoader().getResource(\"%s\").toURI());",
      class_name,
      class_name
    ),
    "\t\t\t\t\t\treturn Repository.loadAll(path, TYPES);",
    "\t\t\t\t} catch (Exception e) { throw new RuntimeException(e); }",
    "\t\t}",
    repo_collection,
    enum_lines,
    all_iface_lines,
    "}"
  )

  writeLines(java_lines, con = java_file)

  message("✅ CSV files and Java interfaces written to: ", directory)
  message("   Java class: ", fqn)

  invisible(list(
    directory = directory,
    fqn = fqn,
    csv_files = df_meta$csv_file,
    java_file = java_file
  ))
}


#' Convert snake_case strings to camelCase or PascalCase
#'
#' @param x A character vector with snake_case names.
#' @param capitalize_first Logical; if TRUE, returns PascalCase (e.g., "MyVariable").
#'                         If FALSE (default), returns camelCase (e.g., "myVariable").
#' @return A character vector with converted names.
#'
#' @examples
#' .to_camel_case("hello_world")
#' .to_camel_case("very_long_variable_name", capitalize_first = TRUE)
#' .to_camel_case("alreadyCamelCase", capitalize_first = TRUE)
#' .to_camel_case(c("hello_world","very_long_variable_name"), capitalize_first = FALSE)
.to_camel_case <- function(x, capitalize_first = TRUE) {
  if (is.null(x) || length(x) == 0 || !is.character(x)) {
    return(x)
  }

  # Handle NA
  if (any(is.na(x))) {
    warning("NAs detected in input; preserving them.")
  }

  # Split on underscores and capitalize each word
  words <- strsplit(x, "_")
  capitalized <- lapply(words, function(parts) {
    parts <- parts[parts != ""] # handle multiple underscores
    if (length(parts) == 0) {
      return("")
    }
    # Capitalize first letter of each part
    cap_part <- .upper1(parts)
    paste(cap_part, collapse = "")
  })

  out <- unlist(capitalized)

  # Convert first character to lowercase unless PascalCase is requested
  if (!capitalize_first && length(out) > 0) {
    out <- sapply(out, .lower1)
  }

  return(unname(out))
}

#' Map R vector type to Java type
#' name is the column name. It will be a lowerCamelCase most likely
#' types is the list of classes. They will be UpperCamelCase
.r_type_to_java_type <- function(x, name, types) {
  fk_match = stringr::str_ends(tolower(name), tolower(types))
  if (any(fk_match)) {
    .upper1(types[fk_match])
  } else if (is.logical(x)) {
    "boolean"
  } else if (is.integer(x)) {
    "int"
  } else if (is.numeric(x)) {
    "double"
  } else if (is.character(x)) {
    "String"
  } else if (is.factor(x)) {
    .upper1(name)
  } else {
    "String" # fallback
  }
}

# .upper1(c("hello","world"))
.upper1 = function(x) {
  return(
    unname(sapply(x, function(s) {
      if (nchar(s) == 0) {
        return(s)
      }
      paste0(
        toupper(substr(s, 1, 1)),
        substr(s, 2, nchar(s))
      )
    }))
  )
}

# .lower1(c("HELLO","WORLD"))
.lower1 = function(x) {
  return(
    unname(sapply(x, function(s) {
      if (nchar(s) == 0) {
        return(s)
      }
      paste0(
        tolower(substr(s, 1, 1)),
        substr(s, 2, nchar(s))
      )
    }))
  )
}

# .to_snake_case(c("male", "female", "non binary"))
# .to_snake_case("nonBinary", FALSE)
.to_snake_case <- function(x, upper = TRUE) {
  x = gsub("[^a-zA-Z0-9]+", "_", x) # replace non-alphanum with _
  x = gsub("([a-z])([A-Z])", "\\1_\\2", x) # replace aaaaBBBB with aaaa_BBBB
  x = gsub("^_+|_+$", "", x) # trim underscores
  x = gsub("_+", "_", x) # rationalise underscores
  if (upper) {
    return(toupper(x))
  }
  return(tolower(x))
}
