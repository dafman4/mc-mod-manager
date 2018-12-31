package com.squedgy.mcmodmanager.api.response;

import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.abstractions.ModMember;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class ModInformation implements CurseForgeResponse {

	private final List<ModVersion> versions = new LinkedList<>();
	private final List<ModMember> members = new LinkedList<>();

	void addMember(ModMember member) {
		this.members.add(member);
	}

	@Override
	public List<ModMember> getMembers() {
		return new LinkedList<>(members);
	}

	@Override
	public ModMember getAuthor() {
		return members.stream()
			.filter(member -> member.getTitle().equalsIgnoreCase("author"))
			.findFirst().orElse(null);
	}

	void addVersion(ModVersion version) {
		this.versions.add(version);
	}

	public List<ModVersion> getVersions() {
		return new LinkedList<>(this.versions);
	}

	@Override
	public List<ModVersion> getRelatedVersions(String mcVersion) {
		return this.versions.stream()
			.filter(v -> v.getMinecraftVersion().equals(mcVersion))
			.collect(Collectors.toList());
	}

	@Override
	public ModVersion getLatestVersion(String mcVersion) {
		return this.versions.stream().max(Comparable::compareTo).orElse(null);
	}

}
