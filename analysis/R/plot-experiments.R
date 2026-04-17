#' 
#' @param tables the output of the `load_duckdb()` function
#' @param ... ggplot format options which can be used to control the plot and 
#'   faceting
#' @param mapping ggplot `aes` mappings which can be used to override the defaults
#' @param facet automatically facet the plot base on the experimental design
#'   if you are not happy with the defaults set this to `FALSE` and add your own 
#'   `facet_wrap()` command
#' @keywords internal
#' @name common-plot
NULL

#' Plot a behaviour area chart.
#'
#' @inheritParams common-plot
#'
#' @returns a ggplot with proportion of behaviours exhibited by the population at any 
#' given time during the simulation.
#' @export
#'
#' @examples
#' tables = load_duckdb("/home/vp22681/Data/ai4ci/0.3.2/behaviour-comparison")
#' tables %>% plot_behaviours()
plot_behaviours = function(tables, ..., mapping = .gg_check_for_aes(...), facet = TRUE) {
  
  mapping = ggplot2::aes(x = time, y = mean_percent, fill=behaviour, !!!mapping)
  facets = if (facet) .default_facets(tables[["dimensions"]], mapping,...) else NULL
  
  data = tables[["behaviours"]]
  
  data = data %>% 
    dplyr::mutate(
      behaviourModel = dplyr::sql("string_split(behaviour,'.')[1]"),
      behaviour = dplyr::sql("string_split(behaviour,'.')[2]")
    ) %>% 
    dplyr::collect() %>%
    dplyr::mutate(behaviour = as.factor(tolower(behaviour)))
  
  browser()
  
  data2 = data %>% 
    .group_by_simulation_day(dimensions = tables[["dimensions"]], replicas=TRUE) %>%
    dplyr::mutate(percent = count/sum(count)) %>%
    .group_by_simulation_day(behaviour, dimensions = tables[["dimensions"]]) %>%
    dplyr::summarise(mean_percent = mean(percent))
  
  ggplot2::ggplot(data2) +
    .gg_layer(
      ggplot2::GeomBar,
      data = data2,
      mapping = mapping,
      position="stack",
      width=1,
      linewidth=0,
      ...,
      .default = list()
    )+facets+.gg_scale_y_percent()+ggplot2::ylab(NULL)
}


#' Plot a policy background layer.
#' 
#' This plots the simulation wide policy in place at any given point in time
#' in the simulation(s). Multiple simulations may have different policies at 
#' different time (e.g. lockdowns will start and end at different times in 
#' different simulations) These can be represented either by a gradual colour 
#' transition in the background (`alpha=TRUE`) or as a stacked bar chart of the
#' number of simulations in a given policy state (`alpha=FALSE`)
#'
#' @inheritParams common-plot
#' @param alpha the default plot is stacked proportions of a particular policy
#'   which would work well as a rug plot underneath a time series. Alternatively
#'   if alpha is `TRUE` the policy state is shown as a solid background with 
#'   varying hues and degrees of alpha. This is useful for the background of 
#'   spaghetti plots of incidence, but this will only be displayed once another
#'   layer is present. In both cases a sensible fill scale needs to be chosen 
#'   that works well with discrete data. 
#' @param palette the palette as a function which takes a number of levels and
#'   returns a colour for that level. In all cases the default (i.e. time zero) 
#'   policy will be transparent white so changes will only be obvious for 
#'   policies that are a change from the baseline.
#' @returns a ggplot timeseries with the background layer holding the policy 
#'   state over time. If `alpha=TRUE` this will only display a meaningful output
#'   if some other layer is providing the y axis.
#' @export
#'
#' @examples
#' tables = load_duckdb("/home/vp22681/Data/ai4ci/0.3.2/behaviour-comparison")
#' plot_policy(tables)
#' plot_policy(tables, alpha = TRUE)+ggplot::geom_hline(yintercept=1)
plot_policy = function(tables, ..., mapping = .gg_check_for_aes(...), facet = TRUE, alpha = FALSE, palette = scales::pal_dichromat("BluetoOrange.12")) {
  
  if (alpha) {
    mapping = ggplot2::aes(xmin = time, xmax=time+1, alpha = mean_percent, fill=policy, !!!mapping)
  } else {
    mapping = ggplot2::aes(x = time, y = mean_percent, fill=policy, !!!mapping)
  }
  
  facets = if (facet) .default_facets(tables[["dimensions"]], mapping,...) else NULL
  
  data = tables[["summary"]]
  
  data = data %>% 
    dplyr::mutate(
      policyModel = dplyr::sql("string_split(policy,'.')[1]"),
      policy = dplyr::sql("string_split(policy,'.')[2]")
    ) %>% 
    dplyr::collect() %>%
    dplyr::mutate(policy = as.factor(tolower(policy)))
  
  # rowser()
  
  data2 = data %>% 
    .group_by_simulation_day(dimensions = tables[["dimensions"]], replicas=TRUE) %>%
    dplyr::mutate(percent = 1/dplyr::n()) %>%
    .group_by_simulation_day(policy, dimensions = tables[["dimensions"]]) %>%
    dplyr::summarise(mean_percent = mean(percent))
  
  levels = data2 %>% dplyr::pull(policy) %>% levels() %>% as.character()
  initial = data2 %>% dplyr::filter(time==min(time)) %>% dplyr::pull(policy) %>% unique() %>% as.character()
  
  pal_wrap = function() {
    n = length(levels)-1
    colours = c("#FFFFFF00", palette(n))
    order = c(initial, setdiff(levels,initial))
    names(colours)=order
    return(scales::pal_manual(
      colours
    ))
  }
  
  if (length(levels) <= 1) {
    message("No changes in policy during simulation. Policy layer not plotted.")
    return(ggplot2::ggplot(data2))
  }
  
  if (alpha) {
    
    # If alpha is involved then the visibitility has to be set to zero for
    # the default level:
    data2 = data2 %>% dplyr::mutate(
      mean_percent = ifelse(policy == initial, 0, mean_percent)
    )
    
    ggplot2::ggplot(data2) +
      .gg_layer(
        ggplot2::GeomRect,
        data = data2,
        mapping = mapping,
        linewidth=0,
        ymin = -Inf,
        ymax = Inf,
        ...,
        .default = list()
      )+facets+
      ggplot2::scale_alpha_identity()+
      ggplot2::scale_fill_discrete(palette=pal_wrap())+
      ggplot2::ylab(NULL)
    
  } else {
    ggplot2::ggplot(data2) +
      .gg_layer(
        ggplot2::GeomBar,
        data = data2,
        mapping = mapping,
        position="stack",
        width=1,
        linewidth=0,
        ...,
        .default = list()
      )+facets+
        .gg_scale_y_percent()+
        ggplot2::scale_fill_discrete(palette=pal_wrap())+
        ggplot2::ylab(NULL)
  }
}

#' Plots summary level information as a time series
#'
#' @inheritParams common-plot
#' @param feature The summary feature to plot. This is a column name of the `tables$summary`
#'   data frame, it can include things like `incidence`, `cumulativeDeaths`
#' @param policy Should we include a background layer showing the policy state
#'   at any given point in time (see `plot_policy()`)?
#' @param spaghetti Should we display multiple time-series as a spaghetti plot 
#'   (each simulation plotted individually as a line) or as a ribbon (mean
#'   value and 0.95 quantiles for all replicas of a given model and experiment)
#' @param points Should we plot the data points as well as the ribbons / lines.
#'
#' @returns a ggplot
#' @export
#'
#' @examples
#' tables = load_duckdb("/home/vp22681/Data/ai4ci/0.3.2/behaviour-comparison")
#' tables$summary %>% dplyr::glimpse()
#' tables %>% plot_summary(averageMobility, spaghetti=FALSE)
#' tables %>% plot_summary(averageViralLoad, spaghetti=FALSE)
#' tables %>% plot_summary(cumulativeDeaths, spaghetti=FALSE)
#' tables %>% plot_summary(hospitalisedCount, spaghetti=FALSE)
#' tables %>% plot_summary(cumulativeInfections, spaghetti=FALSE)
#' tables %>% plot_summary(cumulativeMobilityDecrease, spaghetti=FALSE)
plot_summary = function(tables, feature, ..., mapping = .gg_check_for_aes(...), facet = TRUE, policy=TRUE, spaghetti = TRUE, points = FALSE ) {
  
  feature = rlang::ensym(feature)
  data = tables[["summary"]]
  
  if (policy) {
    out = plot_policy(tables, ..., facet=FALSE, alpha=TRUE)+
      ggnewscale::new_scale_fill()
  } else {
    out = ggplot2::ggplot()
  }
  
  # browser()
  
  if (spaghetti) {
    data2 = data %>% dplyr::collect()
    mapping = ggplot2::aes(x = time, y = !!feature, !!!mapping, group = interaction(modelName,experimentName,modelReplica,experimentReplica))
    out = out + .gg_layer(
      ggplot2::GeomLine,
      data = data2,
      mapping = mapping,
      ...,
      .default = list()
    ) +
      if (points) {
        .gg_layer(
          ggplot2::GeomPoint,
          data = data2,
          mapping = mapping,
          ...,
          .default = list(size=0.05)
        )
      } else {
        NULL
      }
  } else {
    data1 = data %>% dplyr::collect()
    data2 = data %>%
      .group_by_simulation_day(dimensions = tables[["dimensions"]]) %>%
      dplyr::summarise(
        feature_n = dplyr::n(),
        feature_mean = mean(!!feature),
        feature_low = quantile(!!feature, 0.025),
        feature_high = quantile(!!feature, 0.975)
      ) %>% dplyr::collect()
    # If the model is a facet as well then this group interaction is be a bit redundant 
    # but does not affect the output. It is definitely needed if all the plots 
    # are on the same facet through.
    mapping1 = ggplot2::aes(x = time, y = !!feature, !!!mapping, group = interaction(modelName,experimentName))
    mapping2 = ggplot2::aes(x = time, y = feature_mean, !!!mapping, group = interaction(modelName,experimentName))
    mapping3 = ggplot2::aes(x = time, ymin = feature_low, ymax = feature_high, !!!mapping, group = interaction(modelName,experimentName))
    out = out + 
      .gg_layer(
        ggplot2::GeomRibbon,
        data = data2,
        mapping = mapping3,
        linewidth=0,
        alpha=0.1,
        ...,
        .default = list()
      )+
      .gg_layer(
        ggplot2::GeomLine,
        data = data2,
        mapping = mapping2,
        linewidth = 0.25,
        ...,
        .default = list()
      )+
      ggplot2::ylab(rlang::as_label(feature)) +
      if (points) {
        .gg_layer(
          ggplot2::GeomPoint,
          data = data1,
          mapping = mapping1,
          ...,
          .default = list(size=0.05)
        )
      } else {
        NULL
      }
  }
  facets = if (facet) .default_facets(tables[["dimensions"]], mapping,...) else NULL
  return(out+facets)
}

.group_by_simulation_day = function(data, ..., dimensions, replicas = FALSE) {
  s = rlang::ensyms(...)
  s = unname(unlist(sapply(s, rlang::as_label)))
  v = c("time","modelName","experimentName")
  if (replicas) {
    v = c(v,"modelReplica","experimentReplica")
  }
  tmp = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v))) %>% 
    dplyr::summarise(dplyr::across(dplyr::everything(), dplyr::n_distinct)) %>% 
    dplyr::collect() %>% 
    dplyr::ungroup() %>% 
    dplyr::summarise(dplyr::across(dplyr::where(is.numeric), ~ all(.x==1))) %>%
    dplyr::select(!dplyr::any_of(v)) 
  groups = colnames(tmp)[as.logical(tmp)]
  v = unique(c(v,groups,dimensions$names,s))
  
  data = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v)))
  return(data)
}

.default_facets = function(dimensions, mapping, ...) {
  
  mapping_vars = unique(unlist(lapply(mapping, all.vars)))
  cols = setdiff(dimensions$names, mapping_vars)
  dots = rlang::list2(...)
  
  if (length(cols) == 0) {
    # no dots
    return(ggplot2::facet_null())
  } else if (length(cols)==1) {
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_wrap))]
    return(do.call(ggplot2::facet_wrap, c(list(facets = cols), dots)))
  } else if (length(cols)==2) {
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_grid))]
    return(do.call(ggplot2::facet_grid,
      c(list(rows = as.symbol(cols[1]), cols = as.symbol(cols[2])), dots)))
  } else {
    # 3 or more facets... This is likely to be unusable
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_wrap))]
    return(do.call(ggplot2::facet_wrap, c(list(cols), dots)))
  }
  
}