package com.squedgy.mcmodmanager.app.util;

public class Result {

	private final String oldFile;
	private boolean result;
	private String reason;

	public Result(boolean result, String oldFile) {
		this(result, oldFile, "" + result);
	}

	public Result(boolean result, String oldFile, String reason) {
		this.reason = reason;
		this.result = result;
		this.oldFile = oldFile;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		if (result) setReason("Succeeded");
		else setReason("Failed!");
		this.result = result;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "Result{" +
			"result=" + result +
			", reason='" + reason + '\'' +
			'}';
	}

	public String getOldFile() {
		return oldFile;
	}
}
