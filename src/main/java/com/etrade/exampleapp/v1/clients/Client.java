package com.etrade.exampleapp.v1.clients;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public abstract class Client {

	public Client(){}

	public abstract String getHttpMethod();
	public abstract String getURL();

	public String getQueryParam(List<Pair> queryParams) {
		return queryParams.stream()
				.map(pair -> pair.getLeft() + "=" + pair.getRight())
				.collect(Collectors.joining("&"));
	}
}
