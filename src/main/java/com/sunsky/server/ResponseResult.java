package com.sunsky.server;

public class ResponseResult {
	private int code;
	private String body;

	public ResponseResult() {
	}

	public ResponseResult(int code, String body) {
		this.code = code;
		this.body = body;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
