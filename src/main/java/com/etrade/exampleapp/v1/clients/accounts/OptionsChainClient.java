package com.etrade.exampleapp.v1.clients.accounts;

import com.etrade.exampleapp.v1.clients.Client;
import com.etrade.exampleapp.v1.exception.ApiException;
import com.etrade.exampleapp.v1.oauth.AppController;
import com.etrade.exampleapp.v1.oauth.model.ApiResource;
import com.etrade.exampleapp.v1.oauth.model.ContentType;
import com.etrade.exampleapp.v1.oauth.model.Message;
import com.etrade.exampleapp.v1.oauth.model.OauthRequired;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;


public class OptionsChainClient extends Client {
  @Autowired
  AppController oauthManager;
  @Autowired
  ApiResource apiResource;

  public OptionsChainClient(){}

  @Override
  public String getHttpMethod(){
    return "GET";
  }

  @Override
  public String getURL() {
    return String.format("%s%s", apiResource.getApiBaseUrl(), apiResource.getOptionsChainUri());
  }

  /*
   * Client will provide REALTIME quotes only in case of client holding the valid access token/secret(ie, if the user accessed protected resource) and should have
   * accepted the market data agreement on website.
   * if the user  has not authorized the client, this client will return DELAYED quotes.
   */
  public String getOptionsChain(List<Pair> queryParams)  throws ApiException {
    Message message = new Message();

    message.setOauthRequired(OauthRequired.YES);
    message.setHttpMethod(getHttpMethod());
    message.setUrl(getURL());
    message.setQueryString(getQueryParam(queryParams));
    message.setContentType(ContentType.APPLICATION_JSON);

    return oauthManager.invoke(message);
  }
}
