/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 表示API端点信息，从Swagger文档中提取
 */
public class ApiEndpoint {

	private final String path;

	private final String method;

	private final String operationId;

	private final String summary;

	private final String description;

	private final List<ApiParameter> parameters;

	private final ApiResponse response;

	public ApiEndpoint(String path, String method, String operationId, String summary, String description,
			List<ApiParameter> parameters, ApiResponse response) {
		this.path = path;
		this.method = method;
		this.operationId = operationId;
		this.summary = summary;
		this.description = description;
		this.parameters = parameters != null ? parameters : new ArrayList<>();
		this.response = response;
	}

	public String getPath() {
		return path;
	}

	public String getMethod() {
		return method;
	}

	public String getOperationId() {
		return operationId;
	}

	public String getSummary() {
		return summary;
	}

	public String getDescription() {
		return description;
	}

	public List<ApiParameter> getParameters() {
		return parameters;
	}

	public ApiResponse getResponse() {
		return response;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ApiEndpoint that = (ApiEndpoint) o;
		return Objects.equals(path, that.path) && Objects.equals(method, that.method);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, method);
	}

	@Override
	public String toString() {
		return method + " " + path;
	}

}