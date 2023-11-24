package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.ResultInterpreter;
import com.rakovpublic.jneuropallium.worker.net.storages.ReconnectStrategy;
import org.springframework.stereotype.Service;

@Service
public interface ConfigurationService {

     void update(ConfigurationUpdateRequest request);
     IInputService getInputService();

     ReconnectStrategy getReconnectionStrategy();

     ResultInterpreter getResultInterpreter();


}
