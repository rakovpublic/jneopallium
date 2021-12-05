package com.rakovpublic.jneuropallium.master.configs;

import org.springframework.context.annotation.Configuration;

@Configuration
public class NetConfig {
    private String splitInputClass;
    private String inputLoadingStrategyClass;
    private Integer partitions;
    private Integer defaultLoopsCount;
    private String resultRunnerClass;
}
