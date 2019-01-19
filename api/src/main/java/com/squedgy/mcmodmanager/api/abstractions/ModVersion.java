package com.squedgy.mcmodmanager.api.abstractions;

import java.time.LocalDateTime;

public interface ModVersion extends Comparable<ModVersion> {

	String JSON_KEY_FILE_NAME = "file-name",
		JSON_KEY_MOD_NAME = "mod-name",
		JSON_KEY_DOWNLOAD_URL = "download-url",
		JSON_KEY_RELEASE_TYPE = "release-type",
		JSON_KEY_ID = "id",
		JSON_KEY_MINECRAFT_VERSION = "mc-version",
		JSON_KEY_UPLOADED_AT = "uploaded",
		JSON_KEY_MOD_ID = "mod-id",
		JSON_KEY_DESCRIPTION = "desc",
		JSON_KEY_BAD_JAR = "bad-link";

	public abstract String getFileName();

	public abstract String getModName();

	public abstract String getDownloadUrl();

	public abstract String getTypeOfRelease();

	public abstract long getId();

	public abstract String getMinecraftVersion();

	public abstract LocalDateTime getUploadedAt();

	public abstract String getModId();

	public abstract String getDescription();

	public abstract boolean isHtmlDescription();

	public abstract boolean isBadJar();

}
