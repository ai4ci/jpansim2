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
#' with_results_dir(
#'   here("../scratch/examples/behaviour-comparison"),
#'   plot_behaviours()
#' )
plot_behaviours = function(..., mapping = .gg_check_for_aes(...), facet = TRUE) {
  
  mapping = ggplot2::aes(x = time, y = mean_percent, fill=behaviour, !!!mapping)
  facets = if (facet) .default_facets(mapping,...) else NULL
  
  data = get_table("behaviours")
  
  data = data %>% 
    dplyr::mutate(
      behaviour = dplyr::sql("string_split(behaviour,'.')[2]")
    ) %>% 
    dplyr::collect() %>%
    dplyr::mutate(behaviour = forcats::as_factor(tolower(behaviour)))
  
  data2 = data %>% 
    group_by_simulation_day(replicas=TRUE) %>%
    dplyr::mutate(percent = count/sum(count)) %>%
    group_by_simulation_day(behaviour) %>%
    dplyr::summarise(mean_percent = mean(percent)) %>%
    augment_with_parameters()
  
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
#'   if alpha is `TRUE` (or a number > 0) the policy state is shown as a solid 
#'   background with 
#'   varying hues and degrees of alpha. This is useful for the background of 
#'   spaghetti plots of incidence. In both cases a sensible fill scale needs to be chosen 
#'   that works well with discrete data, this must be set with `set_policy_colours()`
#'   or a default which displays lockdowns is used.
#' @param ... not used. 
#' @returns a ggplot timeseries layer with the background layer holding the policy 
#'   state over time.
#' @export
#'
#' @examples
#' set_results_dir(here("../scratch/examples/lockdown-compliance"))
#' ggplot2::ggplot()+geom_policy(alpha = TRUE)+ggplot2::geom_hline(yintercept=1)
#' 
geom_policy = function(alpha = FALSE, ...) {
  
  data = get_table("summary")
  
  data = data %>% 
    dplyr::mutate(
      policy = dplyr::sql("string_split(policy,'.')[2]")
    ) %>% 
    dplyr::collect() %>%
    dplyr::mutate(policy = tolower(policy))
  
  policy_pal = get_policy_colours()
  
  data2 = data %>% 
    group_by_simulation_day(replicas=TRUE) %>%
    dplyr::mutate(percent = 1/dplyr::n()) %>%
    group_by_simulation_day(policy) %>%
    dplyr::summarise(mean_percent = mean(percent)) %>%
    augment_with_parameters() %>%
    # remove non plotted policy items:
    dplyr::filter(policy %in% names(policy_pal)) %>%
    dplyr::mutate(policy = factor(policy, levels=names(policy_pal)))
  
  if (nrow(data2) == 0) {
    message("no policy to show, skipping policy layer")
    return(NULL)
  }
  
  if (!isFALSE(alpha)) {
    
    mapping = ggplot2::aes(xmin = time, xmax=time+1, alpha = mean_percent, fill=policy)
    
    return(
      list(
        .gg_layer(
          ggplot2::GeomRect,
          data = data2 %>% dplyr::mutate(mean_percent = mean_percent*alpha),
          mapping = mapping,
          linewidth=0,
          ymin = -Inf,
          ymax = Inf,
          .default = list()
        ),
        ggplot2::scale_alpha_identity(),
        ggplot2::scale_fill_discrete(palette=scales::pal_manual(values = policy_pal)),
        ggnewscale::new_scale_fill()
      )
    )
  } else {
    
    mapping = ggplot2::aes(x = time, y = mean_percent, fill=policy)
    
    return(
      list(
        .gg_layer(
          ggplot2::GeomBar,
          data = data2,
          mapping = mapping,
          position="stack",
          width=1,
          linewidth=0,
          .default = list()
        ),
        .gg_scale_y_percent(),
        ggplot2::scale_fill_discrete(palette=scales::pal_manual(values = policy_pal)),
        ggnewscale::new_scale_fill()
      )
    )
  }
}

StatQuietQuantile = ggplot2::ggproto(
  "StatQuietQuantile", ggplot2::StatQuantile,
    compute_group = function(self, data, scales, quantiles = c(0.25, 0.5, 0.75), formula = NULL, 
      xseq = NULL, method = "rq", method.args = list(), lambda = 1, 
      na.rm = FALSE) {
         suppressWarnings(suppressMessages(
           ggplot2::ggproto_parent(ggplot2::StatQuantile, self)$compute_group(
         data,scales,quantiles,formula,xseq,method,method.args,lambda,na.rm)
         ))
      })

StatQuietQuantileRibbon = ggplot2::ggproto(
  "StatQuietQuantileRibbon", ggplot2::StatQuantile,
  compute_group = function(self, data, scales, zcrit = 0.95, formula = NULL, 
                           xseq = NULL, method = "rq", method.args = list(), lambda = 1, 
                           na.rm = FALSE) {
    q = 0.5+zcrit/c(-2,2)
    tmp = suppressWarnings(suppressMessages(
      ggplot2::ggproto_parent(ggplot2::StatQuantile, self)$compute_group(
        data=data,scales=scales,quantiles=q,formula=formula,xseq=xseq,method=method,
        method.args=method.args,lambda=lambda,na.rm=na.rm)
    ))
    tmp = tmp %>% dplyr::mutate(
      quantile = dplyr::case_when(
        quantile == min(quantile) ~ "ymin",
        quantile == max(quantile) ~ "ymax",
        FALSE ~ NA
      )) %>% 
      dplyr::select(-group) %>%
      tidyr::pivot_wider(names_from = quantile,values_from = y)
    return(tmp)
  },
  required_aes = c("x", "y"),
  dropped_aes = "y"
  )

#' Plots summary level information as a time series
#'
#' @inheritParams common-plot
#' @param feature The summary feature to plot. This is a column name of the `tables$summary`
#'   data frame, it can include things like `incidence`, `cumulativeDeaths`
#' @param policy Should we include a background layer showing the policy state
#'   at any given point in time (see `plot_policy()`)?
#' @param smooth Should we display multiple time-series as a spaghetti plot 
#'   (each simulation plotted individually as a line) or as a quantile regression (`TRUE`)
#' @param points Should we plot the data points as well as the regressions / lines.
#'
#' @returns a ggplot
#' @export
#'
#' @examples
#' set_results_dir(here("../scratch/examples/lockdown-compliance"))
#' get_table("summary") %>% dplyr::glimpse()
#' plot_summary(averageMobility)
#' 
#' set_results_dir(here("../scratch/examples/behaviour-comparison"))
#' plot_summary(averageViralLoad, smooth=TRUE)
#' plot_summary(cumulativeDeaths, smooth=TRUE)
#' plot_summary(hospitalisedCount, smooth=TRUE)
#' plot_summary(cumulativeInfections, smooth=TRUE)
#' plot_summary(cumulativeMobilityDecrease, smooth=TRUE)
plot_summary = function(feature, ..., mapping = .gg_check_for_aes(...), facet = TRUE, policy=TRUE, smooth = FALSE, points = FALSE ) {
  
  feature = rlang::ensym(feature)
  data = get_table("summary")
  data2 = data  %>%
    augment_with_parameters() %>% 
    dplyr::collect()
  
  fc = .facet_alpha(data2, mapping)
  
  if (policy) {
    out = ggplot2::ggplot()+
      geom_policy(alpha=fc)
  } else {
    out = ggplot2::ggplot()
  }
  
  if (points) {
    map_points = ggplot2::aes(x = time, y = !!feature, !!!mapping)
    out = out+.gg_layer(
      ggplot2::GeomPoint,
      data = data2,
      mapping = map_points,
      alpha=fc,
      ...,
      .default = list(size=0.1)
    )
  }
  
  if (!smooth) {
    
    mapping = ggplot2::aes(x = time, y = !!feature, !!!mapping, group = interaction(modelName,experimentName,modelReplica,experimentReplica))
    
    out = out + 
      .gg_layer(
        ggplot2::GeomLine,
        data = data2,
        mapping = mapping,
        ...,
        .default = list()
      )
  
  } else {
    
    mapping = ggplot2::aes(x = time, y = !!feature, !!!mapping, group = interaction(modelName,experimentName))
    
    out = out + 
      .gg_layer(
        ggplot2::GeomQuantile,
        data = data2,
        mapping = mapping,
        stat = StatQuietQuantile,
        quantiles = c(0.5),
        method="rqss",
        ...,
        .default = list(lambda=5)
      )+
      .gg_layer(
        ggplot2::GeomRibbon,
        data = data2,
        mapping = ..fill_col(mapping),
        stat = StatQuietQuantileRibbon,
        zcrit = 0.5,
        alpha = 0.2,
        method="rqss",
        linetype="3111",
        ...,
        .default = list(lambda=5)
      )+
      .gg_layer(
        ggplot2::GeomRibbon,
        data = data2,
        mapping = ..fill_col(mapping),
        stat = StatQuietQuantileRibbon,
        zcrit = 0.95,
        alpha = 0.1,
        method="rqss",
        linetype="11",
        ...,
        .default = list(lambda=5)
      )+
      ggplot2::ylab(rlang::as_label(feature))
  }
  
  facets = if (facet) .default_facets(mapping, ...) else NULL
  return(out+facets)
}

# .group_by_simulation_day = function(data, ..., dimensions, replicas = FALSE) {
#   s = rlang::ensyms(...)
#   s = unname(unlist(sapply(s, rlang::as_label)))
#   v = c("time","modelName","experimentName")
#   if (replicas) {
#     v = c(v,"modelReplica","experimentReplica")
#   }
#   tmp = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v))) %>% 
#     dplyr::summarise(dplyr::across(dplyr::everything(), dplyr::n_distinct)) %>% 
#     dplyr::collect() %>% 
#     dplyr::ungroup() %>% 
#     dplyr::summarise(dplyr::across(dplyr::where(is.numeric), ~ all(.x==1))) %>%
#     dplyr::select(!dplyr::any_of(v)) 
#   groups = colnames(tmp)[as.logical(tmp)]
#   v = unique(c(v,groups,dimensions$names,s))
#   
#   data = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v)))
#   return(data)
# }

# .group_by_simulation = function(data, ..., dimensions, replicas = FALSE) {
#   s = rlang::ensyms(...)
#   s = unname(unlist(sapply(s, rlang::as_label)))
#   v = c("modelName","experimentName")
#   if (replicas) {
#     v = c(v,"modelReplica","experimentReplica")
#   }
#   # tmp = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v))) %>% 
#   #   dplyr::summarise(dplyr::across(dplyr::everything(), dplyr::n_distinct)) %>% 
#   #   dplyr::collect() %>% 
#   #   dplyr::ungroup() %>% 
#   #   dplyr::summarise(dplyr::across(dplyr::where(is.numeric), ~ all(.x==1))) %>%
#   #   dplyr::select(!dplyr::any_of(v)) 
#   # groups = colnames(tmp)[as.logical(tmp)]
#   # v = unique(c(v,groups,dimensions$names,s))
#   v = unique(c(v,dimensions$names,s))
#   
#   data = data %>% dplyr::group_by(dplyr::across(dplyr::any_of(v)))
#   return(data)
# }

.facet_alpha = function(data, mapping, ...) {
  dimensions = get_table("dimensions")
  data = data %>% dplyr::ungroup()
  mapping_vars = unique(unlist(lapply(mapping, all.vars)))
  cols = setdiff(dimensions$comparisonNames, mapping_vars)
  if (length(cols)==2) {
    tmpc = data %>% dplyr::select(dplyr::any_of(cols[2])) %>% dplyr::distinct() %>% nrow()
    tmpr = data %>% dplyr::select(dplyr::any_of(cols[1])) %>% dplyr::distinct() %>% nrow()
    tmp = tmpc * tmpr
  } else {
    tmp = data %>% dplyr::select(dplyr::any_of(cols)) %>% dplyr::distinct() %>% nrow()
  }
  return(tmp/dimensions$simulations)
}

.default_facets = function(mapping, ...) {
  
  dimensions = get_table("dimensions")
  
  mapping_vars = unique(unlist(lapply(mapping, all.vars)))
  cols = setdiff(dimensions$comparisonNames, mapping_vars)
  dots = rlang::list2(...)
  
  
  if (length(cols) == 0) {
    # no dots
    return(ggplot2::facet_null())
  } else if (length(cols)==1) {
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_wrap))]
    return(do.call(ggplot2::facet_wrap, c(list(facets = cols), dots)))
  } else if (length(cols)==2) {
    message("facet grid: ",paste0(cols,collapse = ", "))
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_grid))]
    return(
      do.call(ggplot2::facet_grid,
        c(
          list(
            rows = as.formula(paste0(cols[1],"~",cols[2])), 
            cols = NULL
          ),
          dots)
      )
    )
  } else {
    message("facet wrap: ",paste0(cols,collapse = ", "))
    # 3 or more facets... This is likely to be unusable
    dots = dots[names(dots) %in% names(formals(ggplot2::facet_wrap))]
    return(do.call(ggplot2::facet_wrap, c(list(cols), dots)))
  }
  
}


#' Assemble a grid from a list of one dimension faceted plots
#' 
#' When plotting a set of facetted timeseries with different y axes it is useful
#' to be able to easily stack them on top of each other removing x axes and facet
#' labels for everything but the first and last. This function also enforces a
#' single row, and collects and positions the legends at the bottom of the plots.
#'
#' @param ... a set of ggplots
#'
#' @returns a patchwork of the ggplots with 
#' @export
#'
#' @examples
#' set_results_dir(here("../scratch/examples/behaviour-comparison"))
#' grid_layout(
#'   plot_summary(cumulativeInfections, smooth=TRUE),
#'   plot_summary(cumulativeMobilityDecrease, smooth=TRUE)
#' )
grid_layout = function(...) {
  plots = rlang::list2(...)
  
  thm_not_first = ggplot2::theme(strip.background.x = ggplot2::element_blank(), strip.text.x.top = ggplot2::element_blank())
  thm_not_last = ggplot2::theme(axis.text.x.bottom = ggplot2::element_blank(), axis.title.x.bottom = ggplot2::element_blank())
  
  for (i in seq_along(plots)) {
    plots[[i]]@facet$params$nrow=1
    plots[[i]] = plots[[i]]+ggplot2::theme(legend.position = "bottom", legend.direction = "horizontal")
    if (i != 1) plots[[i]] = plots[[i]]+thm_not_first
    if (i != length(plots)) plots[[i]] = plots[[i]]+thm_not_last
  }
  
  return(patchwork::wrap_plots(plots,ncol=1,guides = "collect")&ggplot2::theme(legend.position = "bottom", legend.direction = "horizontal"))
}


plot_contacts = function( ..., mapping = .gg_check_for_aes(...), facet = TRUE) {
  contacts = get_table("contact_counts") %>% dplyr::filter(time>0) %>%
    group_by_simulation_day(contacts) %>%
    dplyr::summarise(count=mean(count)) %>%
    augment_with_parameters()
  mapping = ggplot2::aes(xmin=time-1, xmax=time,ymin=contacts+1, ymax=contacts+2,fill=count,!!!mapping)
  out = ggplot2::ggplot(contacts, mapping)+ggplot2::geom_rect()+ggplot2::scale_fill_viridis_c()+
    .gg_scale_y_log1p()
  facets = if (facet) .default_facets(mapping, ...) else NULL
  return(out+facets)
  
  # if (!rayshader) return(out+facets)
  # 
  # rayshader::plot_gg(
  #   out+facets,
  #   width = 5,
  #   height = 5,
  #   multicore = TRUE,
  #   scale = 250,
  #   zoom = 0.7,
  #   theta = 10,
  #   phi = 30,
  #   windowsize = c(800, 800)
  # )
  # 
  # rayshader::render_snapshot(clear = TRUE, plot = FALSE)
}