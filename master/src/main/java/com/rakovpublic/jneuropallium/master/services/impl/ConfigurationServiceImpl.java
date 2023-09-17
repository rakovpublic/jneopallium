/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.IInputService;


//TODO: add implementation
public class ConfigurationServiceImpl implements ConfigurationService {
    private IInputService inputService;




    @Override
    public void update(ConfigurationUpdateRequest request) {

        updateInputService(request);
    }

    @Override
    public IInputService getInputService() {
        return inputService;
    }

    private void updateInputService(ConfigurationUpdateRequest configuration){

    }

}
