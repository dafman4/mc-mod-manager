package com.squedgy.mcmodmanager.api.response;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

public class ModVersionFactory {

    private String name;
    private String fileName;

    public ModVersionFactory(){ }

    public ModVersionFactory withName(String name) {
        if(name != null) this.name = name;
        return this;
    }

    public ModVersionFactory withFileName(String name) {
        if(name != null) this.fileName = name;
        return this;
    }

    public ModVersion build(){
        Version ret = new Version();
        ret.setModName(name);
        ret.setFileName(fileName);
        return ret;
    }

}
