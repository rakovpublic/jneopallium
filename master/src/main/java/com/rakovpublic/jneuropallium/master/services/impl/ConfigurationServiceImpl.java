/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.worker.net.storages.ReconnectStrategy;


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

    @Override
    public ReconnectStrategy getReconnectionStrategy() {
        return null;
    }

    private void updateInputService(ConfigurationUpdateRequest configuration){

    }

}
