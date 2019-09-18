package com.etrade.exampleapp.v1.oauth;

public class TokenException extends RuntimeException {
	private String message;

	public TokenException(Exception e, String message) {
		super(e);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
