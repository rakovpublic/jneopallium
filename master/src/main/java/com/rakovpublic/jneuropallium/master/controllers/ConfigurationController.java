package com.rakovpublic.jneuropallium.master.controllers;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigurationController {
    @PostMapping("/update")
    public void update(@RequestBody ConfigurationUpdateRequest request){


    }
}
