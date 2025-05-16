/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.parser;

import io.swagger.v3.oas.models.media.Schema;

/**
 * 表示API参数信息，从Swagger文档中提取
 */
public class ApiParameter {

	private final String name;

	private final String in; // query, path, header, cookie

	private final boolean required;

	private final Schema<?> schema;

	private final String description;

	public ApiParameter(String name, String in, boolean required, Schema<?> schema, String description) {
		this.name = name;
		this.in = in;
		this.required = required;
		this.schema = schema;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getIn() {
		return in;
	}

	public boolean isRequired() {
		return required;
	}

	public Schema<?> getSchema() {
		return schema;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return name + " (" + in + (required ? ", required" : "") + ")";
	}

}