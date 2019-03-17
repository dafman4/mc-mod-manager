package com.squedgy.mcmodmanager.api.response;

public class ModIdFoundConnectionFailed extends Exception {

	public ModIdFoundConnectionFailed() {
	}

	public ModIdFoundConnectionFailed(String message) {
		super(message);
	}

	public ModIdFoundConnectionFailed(String message, Throwable cause) {
		super(message, cause);
	}
}
