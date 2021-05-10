package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.IInputService;

import java.util.HashMap;

public class NetsHolder {
    private HashMap<String,IInputService> netsMap;
    private static NetsHolder netsHolder = new NetsHolder();
    private NetsHolder(){
        netsMap = new HashMap<>();
    }

    public static NetsHolder getNetsHolder(){
        return netsHolder;
    }

    public IInputService getNetInputService( String netName){
        return netsMap.get(netName);
    }

    public void putNetInput(String netName, IInputService iInputService){
        netsMap.put(netName,iInputService);
    }
}
