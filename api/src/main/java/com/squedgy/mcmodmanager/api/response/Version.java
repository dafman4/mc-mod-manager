package com.squedgy.mcmodmanager.api.response;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.time.LocalDateTime;

public class Version implements ModVersion{

    private long id;
    private String fileName;
    private String typeOfRelease;
    private String minecraftVersion;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
    private String modId;
    private String modName;
    private String description;

    public String getMinecraftVersion() { return minecraftVersion; }

    void setMinecraftVersion(String minecraftVersion) { this.minecraftVersion = minecraftVersion; }

    @Override
    public LocalDateTime getUploadedAt() { return this.uploadedAt; }

    void setUploadedAt(LocalDateTime uploadedAt){ this.uploadedAt = uploadedAt; }

    public String getFileName() { return fileName; }

    void setFileName(String fileName) { this.fileName = fileName; }

    public String getTypeOfRelease() { return typeOfRelease; }

    void setTypeOfRelease(String typeOfRelease) { this.typeOfRelease = typeOfRelease; }

    public long getId() { return id; }

    void setId(long id) { this.id = id; }

    public String getDownloadUrl() { return downloadUrl; }

    void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    @Override
    public int compareTo(ModVersion o) { return uploadedAt.compareTo(o.getUploadedAt()); }

    @Override
    public String getModName() { return modName; }

    @Override
    public String getModId() { return modId; }

    @Override
    public String getDescription() { return description; }

    @Override
    public boolean isHtmlDescription() { return true; }

    public void setDescription(String description) { this.description = description; }

    public void setModName(String modName) { this.modName = modName; }

    public void setModId(String modId) { this.modId = modId; }

    @Override
    public String toString() {
        return "Version{" +
                "minecraftVersion='" + minecraftVersion + '\'' +
                ",\n\tfileName='" + fileName + '\'' +
                ",\n\ttypeOfRelease='" + typeOfRelease + '\'' +
                ",\n\tid=" + id +
                ",\n\tdownloadUrl='" + downloadUrl + '\'' +
                ",\n\tuploadedAt=" + uploadedAt +
                ",\n\tmodName='" + modName + '\'' +
                ",\n\tmodId='" + modId + '\'' +
                ",\n\tdescription='" + description + '\'' +
                '\n' + '}';
    }
}
