package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import org.springframework.stereotype.Service;

@Service
public interface ConfigurationService {

     void update(ConfigurationUpdateRequest request);
     IInputService getInputService();
}
