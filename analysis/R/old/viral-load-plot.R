ggplot2::ggplot(tmp$viral_challenge %>% dplyr::filter(time<50) %>% dplyr::group_by(experimentName) %>% 
                  dplyr::rename(`viral load`=normalisedViralLoad, `immunity`=immuneActivity, `severity` = normalisedSeverity) %>%
                  tidyr::pivot_longer(cols=c(`viral load`, `immunity`, `severity`),names_to = "metric") %>%
                  dplyr::collect() %>%
                  dplyr::mutate(
                    metric = factor(metric, levels = c("viral load", "immunity", "severity")),
                    experimentName = stringr::str_replace_all(experimentName,"^[^:]+:","")
                  ), 
                ggplot2::aes(x=time, group=personId))+
  ggplot2::geom_line(ggplot2::aes(y=value, colour=metric), alpha=0.1)+
  ggplot2::facet_grid(metric~experimentName,scales = "free_y")+
  ggplot2::scale_colour_discrete(guide = ggplot2::guide_none()) #(override.aes = list(size = 3,                         alpha = 1) ) )

