package com.squedgy.mcmodmanager.app.util;

public class Result {

    private boolean result;
    private String reason;

    public Result(boolean result){ this(result, "" + result); }

    public Result(boolean result, String reason){
        this.reason = result ? reason : "failed: " + reason;
        this.result = result;
    }

    public boolean isResult() { return result; }

    public void setResult(boolean result) {
        if(result) setReason("Succeeded");
        else setReason("Failed!");
        this.result = result;
    }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }
}
