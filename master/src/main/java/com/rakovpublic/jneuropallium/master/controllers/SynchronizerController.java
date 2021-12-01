package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SynchronizerController {
    public void nodeComplete(){
        //MasterContext.getMasterContext().getInputService().uploadWorkerResult();
    }
    public void getNextRun(){
       // MasterContext.getMasterContext().getInputService().getNext();
    }

}
