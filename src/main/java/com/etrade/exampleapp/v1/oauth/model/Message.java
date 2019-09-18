package com.etrade.exampleapp.v1.oauth.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Setter
public class Message {
	private OauthRequired oauthRequired;
	private String url;
	private String verifierCode;
	private MultiValueMap<String, String> headerMap = new LinkedMultiValueMap<>();
	private String queryString;
	private String httpMethod;
	private ContentType contentType;
	private String body;
	private String oauthHeader;
}
