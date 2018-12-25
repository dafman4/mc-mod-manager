package com.squedgy.mcmodmanager.api.response;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.time.LocalDateTime;

public class ModVersionFactory {

    private String name;
    private String fileName;
    private String url;
    private String type;
    private long id;
    private String version;
    private LocalDateTime uploadedAt;
    private String modId;

    public ModVersionFactory(){ }

    public ModVersionFactory withName(String name) {
        if(name != null) this.name = name;
        return this;
    }

    public ModVersionFactory withFileName(String name) {
        if(name != null) this.fileName = name;
        return this;
    }

    public ModVersionFactory withUrl(String url){
        if(url != null) this.url = url;
        return this;
    }

    public ModVersionFactory withType(String type){
        if(type != null) this.type = type;
        return this;
    }

    public ModVersionFactory withId(long id){
        this.id = id;
        return this;
    }

    public ModVersionFactory withMcVersion(String version){
        if(version != null) this.version = version;
        return this;
    }

    public ModVersionFactory uploadedAt(LocalDateTime uploadedAt){
        if(uploadedAt != null) this.uploadedAt = uploadedAt;
        return this;
    }

    public ModVersionFactory withModId(String modId){
        if(modId != null) this.modId = modId;
        return this;
    }

    public ModVersion build(){
        Version ret = new Version();
        ret.setModName(name);
        ret.setFileName(fileName);
        ret.setDownloadUrl(url);
        ret.setTypeOfRelease(type);
        ret.setId(id);
        ret.setMinecraftVersion(version);
        ret.setUploadedAt(uploadedAt);
        ret.setModId(modId);
        return ret;
    }

}
