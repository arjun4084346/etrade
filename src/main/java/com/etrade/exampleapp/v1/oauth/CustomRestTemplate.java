package com.etrade.exampleapp.v1.oauth;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.etrade.exampleapp.v1.exception.ApiException;
import com.etrade.exampleapp.v1.oauth.model.Message;

/*
 * Rest template that is able to make rest call by adding oauth related headers
 *
 */
@Slf4j
public class CustomRestTemplate  extends RestTemplate {

	public CustomRestTemplate(ClientHttpRequestFactory factory) {
		super(factory);
	}

	<T> ResponseEntity<LinkedMultiValueMap> execute(Message message) throws ApiException {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		headers.add("Content-type", MediaType.APPLICATION_FORM_URLENCODED.toString());
		log.debug("Oauth Header"+message.getOauthHeader());

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(message.getUrl())/*.queryParams(message.getHeaderMap())*/.build();

		headers.add("Authorization", message.getOauthHeader());

		log.debug( " Message : "+ uriComponents.toString());
		ResponseEntity<LinkedMultiValueMap> response = null;

		try {
			HttpEntity<String> entity = new HttpEntity<>(headers);
			response = super.exchange(uriComponents.toString(), HttpMethod.valueOf(message.getHttpMethod()), entity, LinkedMultiValueMap.class);
		} catch(Exception e) {
			log.error("Failed calling service",e);
			log.error("Exception class name " + e.getCause().getClass().getSimpleName());

			if (ApiException.class.isAssignableFrom(e.getCause().getClass())) {
				log.error(" ApiException found ");
				throw (ApiException)(e.getCause());
			} else {
				throw new ApiException(500, "500", "Internal Failure");
			}
		}
		log.debug(" Completed the service call");
		return response;
	}

	String doExecute(Message message) {
		String jsonResponse = "";
		String url;

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (StringUtils.isNotBlank(message.getOauthHeader())) {
			headers.add("Authorization", message.getOauthHeader());
			log.debug("Oauth Header"+message.getOauthHeader());
		}

		if (StringUtils.isNotBlank(message.getQueryString())) {
			url = String.format("%s?%s", message.getUrl(),message.getQueryString());
		} else {
			url = message.getUrl();
		}

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(url).queryParams(message.getHeaderMap()).build();

		if ("GET".equalsIgnoreCase(message.getHttpMethod())) {
			HttpEntity<String> entity = new HttpEntity<>(headers);
			ResponseEntity<String> response = super.exchange(url, HttpMethod.GET, entity, String.class);
			return response.getBody();
		} else if("POST".equalsIgnoreCase(message.getHttpMethod())) {
			HttpEntity<String> request = new HttpEntity<>(message.getBody(), headers);
			jsonResponse = super.postForObject(uriComponents.toString(), request, String.class);
		}

		return jsonResponse;
	}
}
