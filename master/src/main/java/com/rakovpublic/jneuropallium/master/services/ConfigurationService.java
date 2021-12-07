package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;

public interface ConfigurationService extends Service{

    public void update(ConfigurationUpdateRequest request);
}
