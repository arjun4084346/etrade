package com.etrade.exampleapp.v1.clients;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

public abstract class Client {
	protected Logger log = Logger.getLogger(Client.class);

	public Client(){}

	public abstract String getHttpMethod();
	public abstract String getURL();

	public String getQueryParam(List<Pair> queryParams) {
		return queryParams.stream()
				.map(pair -> pair.getLeft() + "=" + pair.getRight())
				.collect(Collectors.joining("&"));
	}
}
