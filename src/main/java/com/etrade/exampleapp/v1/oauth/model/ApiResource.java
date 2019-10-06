package com.etrade.exampleapp.v1.oauth.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResource {
	private String apiBaseUrl;
	private String acctListUri;
	private String balanceUri;
	private String queryParam;
	private String portfolioUri;
	private String quoteUri;
	private String sandboxBaseUrl;
	private String orderListUri;
	private String orderPreviewUri;
	private String optionsChainUri;
}
