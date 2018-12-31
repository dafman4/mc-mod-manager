package com.squedgy.mcmodmanager.api.response;

public class ModIdFailedException extends RuntimeException {

	public ModIdFailedException() {
	}

	public ModIdFailedException(String mes) {
		super(mes);
	}

}
