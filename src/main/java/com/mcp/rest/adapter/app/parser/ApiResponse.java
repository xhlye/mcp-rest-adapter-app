/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.mcp.rest.adapter.app.parser;

import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.Map;

/**
 * 表示API响应信息，从Swagger文档中提取
 */
public class ApiResponse {

	private final Map<String, Schema<?>> schemas;

	public ApiResponse() {
		this(new HashMap<>());
	}

	public ApiResponse(Map<String, Schema<?>> schemas) {
		this.schemas = schemas != null ? schemas : new HashMap<>();
	}

	public Map<String, Schema<?>> getSchemas() {
		return schemas;
	}

	@Override
	public String toString() {
		return "Response with " + schemas.size() + " schemas";
	}

}