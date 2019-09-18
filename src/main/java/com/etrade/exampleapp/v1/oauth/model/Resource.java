package com.etrade.exampleapp.v1.oauth.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Resource {
	private String consumerKey;
	private String sharedSecret;
	private String requestTokenUrl;
	private String authorizeUrl;
	private String accessTokenUrl;
	private Signer  signatureMethod;
	private String requestTokenHttpMethod = "GET";
	private String accessTokenHttpMethod = "GET";
	private String apiBaseUrl;
}
