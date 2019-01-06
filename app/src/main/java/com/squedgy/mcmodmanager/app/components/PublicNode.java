package com.squedgy.mcmodmanager.app.components;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.app.util.ModUtils;

import java.util.Map;

public class PublicNode {

	private final ModVersion key;
	private final String value;

	public PublicNode(Map.Entry<ModUtils.IdResult, String> entry) {
		key = entry.getKey().mod;
		value = entry.getValue();
	}

	public ModVersion getKey() { return key; }

	public String getValue() { return value; }

	@Override
	public String toString() {
		return "PublicNode{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
