/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;

public class ConfigurationServiceImpl implements ConfigurationService {
    private ConfigurationUpdateRequest configuration;





    @Override
    public void update(ConfigurationUpdateRequest request) {
        configuration = request;
    }

}
