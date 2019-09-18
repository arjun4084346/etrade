package com.etrade.exampleapp.v1.exception;

import java.io.IOException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiException extends IOException {
	private static final long serialVersionUID = 15849534857845L;
	private int httpStatus;
	private String code;
	private String message;

	public ApiException() {
		super();
	}

	public ApiException(String message) {
		super();
		this.message = message;
	}

	public ApiException(final int httpStatus,final String code, final String message) {
		super();
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
