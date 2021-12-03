package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.services.MasterContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classloader")
public class ClassLoaderController {
    public void persistAndLoadClasses(){
        //MasterContext.getMasterContext().loadClass();
    }
    public void getJarsToLoad(){
        //MasterContext.getMasterContext().getJars();
    }
}
