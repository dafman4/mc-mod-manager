package com.squedgy.mcmodmanager.api.response;

public class ModIdNotFoundException extends RuntimeException {

	public ModIdNotFoundException(String mess) {
		super(mess);
	}

	public ModIdNotFoundException(String mess, Throwable cause) {
		super(mess, cause);
	}

}
