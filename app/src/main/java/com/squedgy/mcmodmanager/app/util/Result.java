package com.squedgy.mcmodmanager.app.util;

public class Result {

	private boolean result;
	private String reason;

	public Result(boolean result, String reason) {
		this.reason = reason;
		this.result = result;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
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
}
