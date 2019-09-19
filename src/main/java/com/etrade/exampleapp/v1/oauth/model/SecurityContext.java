package com.etrade.exampleapp.v1.oauth.model;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecurityContext extends HashMap<String, OAuthToken> {
	private Resource resouces;
	private boolean intialized;

	public OAuthToken getToken() {
		return super.get("TOKEN");
	}
}
