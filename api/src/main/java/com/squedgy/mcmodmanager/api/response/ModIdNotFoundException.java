package com.squedgy.mcmodmanager.api.response;

public class ModIdNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "There was an issue with the mod for ";
    private final String id;

    public ModIdNotFoundException(String id) { super(DEFAULT_MESSAGE + id); this.id = id; }

    public ModIdNotFoundException(String id, Throwable cause) { super(DEFAULT_MESSAGE + id, cause);  this.id = id; }

    @Override
    public String getMessage() { return super.getMessage(); }

    public String getModId(){ return id; }
}
