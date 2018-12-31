package com.squedgy.mcmodmanager.api.abstractions;

import java.util.List;

public interface CurseForgeResponse {

	public abstract List<ModMember> getMembers();

	public abstract ModMember getAuthor();

	public abstract List<ModVersion> getVersions();

	public abstract List<ModVersion> getRelatedVersions(String mcVersion);

	public abstract ModVersion getLatestVersion(String mcVersion);

}
