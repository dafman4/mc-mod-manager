package com.squedgy.mcmodmanager.api.response;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.time.LocalDateTime;
import java.util.Objects;

public class Version implements ModVersion {

	private long id;
	private String fileName;
	private String typeOfRelease;
	private String minecraftVersion;
	private String downloadUrl;
	private LocalDateTime uploadedAt;
	private String modId;
	private String modName;
	private String description;

	public String getMinecraftVersion() {
		return minecraftVersion;
	}

	void setMinecraftVersion(String minecraftVersion) {
		this.minecraftVersion = minecraftVersion;
	}

	@Override
	public LocalDateTime getUploadedAt() {
		return this.uploadedAt;
	}

	void setUploadedAt(LocalDateTime uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public String getFileName() {
		return (fileName == null ? fileName : fileName.replace('+', ' '));
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getTypeOfRelease() {
		return typeOfRelease;
	}

	void setTypeOfRelease(String typeOfRelease) {
		this.typeOfRelease = typeOfRelease;
	}

	public long getId() {
		return id;
	}

	void setId(long id) {
		this.id = id;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	@Override
	public int compareTo(ModVersion o) {
		return uploadedAt.compareTo(o.getUploadedAt());
	}

	@Override
	public String getModName() {
		return modName;
	}

	public void setModName(String modName) {
		this.modName = modName;
	}

	@Override
	public String getModId() {
		return modId;
	}

	public void setModId(String modId) {
		this.modId = modId;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public boolean isHtmlDescription() {
		return true;
	}

	@Override
	public String toString() {
		return "Version{" +
			"minecraftVersion='" + minecraftVersion + '\'' +
			",fileName='" + fileName + '\'' +
			",typeOfRelease='" + typeOfRelease + '\'' +
			",id=" + id +
			",downloadUrl='" + downloadUrl + '\'' +
			",uploadedAt=" + uploadedAt +
			",modName='" + modName + '\'' +
			",modId='" + modId + '\'' +
			",description='" + (description != null ? description.replace('\n', ' ') : "null") + '\''
			+ '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass().equals(o.getClass())) return false;
		Version version = (Version) o;
		return (Objects.equals(modId, version.modId)) &&
			Objects.equals(modName, version.modName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(modId, modName);
	}
}
