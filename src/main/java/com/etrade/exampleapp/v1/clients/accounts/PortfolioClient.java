package com.etrade.exampleapp.v1.clients.accounts;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import com.etrade.exampleapp.v1.clients.Client;
import com.etrade.exampleapp.v1.exception.ApiException;
import com.etrade.exampleapp.v1.oauth.AppController;
import com.etrade.exampleapp.v1.oauth.model.ApiResource;
import com.etrade.exampleapp.v1.oauth.model.ContentType;
import com.etrade.exampleapp.v1.oauth.model.Message;
import com.etrade.exampleapp.v1.oauth.model.OauthRequired;
import com.etrade.exampleapp.v1.terminal.AppConfig;

import com.google.common.collect.Lists;

/*
 *
 * Client fetches the portfolio details for selected accountIdKey available with account list.
 * client uses oauth_token & oauth_token_secret to access protected resources that is available via oauth handshake.
 */
@Slf4j
public class PortfolioClient extends Client {
	@Autowired
	AppController oauthManager;
	@Autowired
	ApiResource apiResource;
	public PortfolioClient(){}

	@Override
	public String getHttpMethod(){
		return "GET";
	}

	public String getURL(String accountIdkKey) {
        return String.format("%s%s%s",
						getURL(),
						accountIdkKey == null ? apiResource.getAccountIdKey() : accountIdkKey,
						"/portfolio");
	}

	public String getQueryParam() {
		return null;
	}

	@Override
	public String getURL() {
		return String.format("%s%s", apiResource.getApiBaseUrl(), apiResource.getPortfolioUri());
	}

	public String getPortfolio() throws ApiException {
		return getPortfolio(null, Lists.newArrayList());
	}

	public String getPortfolio(String accountIdKey) throws ApiException {
		return getPortfolio(accountIdKey, Lists.newArrayList());
	}

	public String getPortfolio(List<Pair> queryParams) throws ApiException {
		return getPortfolio(null, queryParams);
	}

	public String getPortfolio(final String accountIdKey, List<Pair> queryParams) throws ApiException{
		log.debug(" Calling Portfolio API " + getURL(accountIdKey));

		queryParams.add(Pair.of("sortBy", "SYMBOL"));
    queryParams.add(Pair.of("view", "COMPLETE"));
    queryParams.add(Pair.of("count", AppConfig.maxNumOfPositions));

		Message message = new Message();
		message.setOauthRequired(OauthRequired.YES);
		message.setHttpMethod(getHttpMethod());
		message.setUrl(getURL(accountIdKey));
		message.setQueryString(getQueryParam(queryParams));
		message.setContentType(ContentType.APPLICATION_JSON);

		return oauthManager.invoke(message);
	}
}
