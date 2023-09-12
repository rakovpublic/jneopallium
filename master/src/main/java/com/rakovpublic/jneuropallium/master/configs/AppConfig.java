/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.configs;

import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.impl.InputServiceConfigResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AppConfig {
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public IInputService getInputService(){
        return InputServiceConfigResolver.initService();
    }
}
