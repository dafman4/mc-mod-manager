package com.squedgy.mcmodmanager.api.abstractions;

import java.time.LocalDateTime;

public interface ModVersion extends Comparable<ModVersion>{

    public abstract String getFileName();
    public abstract String getTypeOfRelease();
    public abstract long getId();
    public abstract String getDownloadUrl();
    public abstract String getMinecraftVersion();
    public abstract LocalDateTime getUploadedAt();
    public abstract String getModName();
    public abstract String getModId();

}
