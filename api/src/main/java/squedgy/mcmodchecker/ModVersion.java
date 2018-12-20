package squedgy.mcmodchecker;

public class ModVersion {

    private String minecraftVersion;
    private String fileName;
    private String typeOfRelease;
    private long id;
    private String downloadUrl;

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTypeOfRelease() {
        return typeOfRelease;
    }

    public void setTypeOfRelease(String typeOfRelease) {
        this.typeOfRelease = typeOfRelease;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
