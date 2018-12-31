package com.squedgy.mcmodmanager.app.components;

import java.util.Map;

public class PublicNode {

	private final String key;
	private final String value;

	public PublicNode(Map.Entry<String, String> entry) {
		key = entry.getKey();
		value = entry.getValue();
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "PublicNode{" +
			"key='" + key + '\'' +
			", value='" + value + '\'' +
			'}';
	}
}
