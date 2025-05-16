/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.parser;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析Swagger/OpenAPI文档，提取API端点信息
 */
public class SwaggerParser {

	private static final Logger logger = LoggerFactory.getLogger(SwaggerParser.class);

	private final OpenAPI openAPI;

	/**
	 * 从JSON文档创建解析器
	 */
	public SwaggerParser(String swaggerJson) {
		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setResolveFully(true);
		this.openAPI = new OpenAPIParser().readContents(swaggerJson, null, options).getOpenAPI();

		if (this.openAPI == null) {
			throw new IllegalArgumentException("Invalid Swagger/OpenAPI document");
		}
	}

	/**
	 * 从文件路径创建解析器
	 */
	public static SwaggerParser fromFile(String filePath) throws IOException {
		Path path = Paths.get(filePath);
		String content = Files.readString(path);
		return new SwaggerParser(content);
	}

	/**
	 * 提取API端点信息
	 */
	public List<ApiEndpoint> extractEndpoints() {
		List<ApiEndpoint> endpoints = new ArrayList<>();

		if (openAPI.getPaths() == null) {
			logger.warn("No paths found in OpenAPI document");
			return endpoints;
		}

		for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
			String path = pathEntry.getKey();
			PathItem pathItem = pathEntry.getValue();

			// 处理GET方法
			if (pathItem.getGet() != null) {
				endpoints.add(extractEndpoint(path, "GET", pathItem.getGet()));
			}

			// 处理POST方法
			if (pathItem.getPost() != null) {
				endpoints.add(extractEndpoint(path, "POST", pathItem.getPost()));
			}

			// 处理PUT方法
			if (pathItem.getPut() != null) {
				endpoints.add(extractEndpoint(path, "PUT", pathItem.getPut()));
			}

			// 处理DELETE方法
			if (pathItem.getDelete() != null) {
				endpoints.add(extractEndpoint(path, "DELETE", pathItem.getDelete()));
			}

			// 处理PATCH方法
			if (pathItem.getPatch() != null) {
				endpoints.add(extractEndpoint(path, "PATCH", pathItem.getPatch()));
			}
		}

		return endpoints;
	}

	private ApiEndpoint extractEndpoint(String path, String method, Operation operation) {
		String operationId = operation.getOperationId();
		String summary = operation.getSummary();
		String description = operation.getDescription();

		if (description == null && summary != null) {
			description = summary;
		}
		else if (description == null) {
			description = path;
		}

		// 提取参数信息
		List<ApiParameter> parameters = extractParameters(operation);

		// 提取响应信息
		ApiResponse response = extractResponse(operation);

		return new ApiEndpoint(path, method, operationId, summary, description, parameters, response);
	}

	private List<ApiParameter> extractParameters(Operation operation) {
		List<ApiParameter> apiParameters = new ArrayList<>();

		// 处理路径、查询、头部和cookie参数
		if (operation.getParameters() != null) {
			for (Parameter parameter : operation.getParameters()) {
				apiParameters.add(new ApiParameter(parameter.getName(), parameter.getIn(),
						Boolean.TRUE.equals(parameter.getRequired()), parameter.getSchema(),
						parameter.getDescription()));
			}
		}

		// 处理请求体参数
		RequestBody requestBody = operation.getRequestBody();
		if (requestBody != null && requestBody.getContent() != null) {
			Content content = requestBody.getContent();

			// 优先处理application/json
			MediaType jsonMediaType = content.get("application/json");
			if (jsonMediaType != null && jsonMediaType.getSchema() != null) {
				Schema<?> schema = jsonMediaType.getSchema();
				apiParameters.add(new ApiParameter("body", "body", Boolean.TRUE.equals(requestBody.getRequired()),
						schema, requestBody.getDescription()));
			}
		}

		return apiParameters;
	}

	private ApiResponse extractResponse(Operation operation) {
		Map<String, Schema<?>> schemas = new HashMap<>();

		ApiResponses responses = operation.getResponses();
		if (responses != null) {
			// 优先处理200和201状态码
			io.swagger.v3.oas.models.responses.ApiResponse successResponse = responses.get("200") != null
					? responses.get("200") : responses.get("201");

			if (successResponse != null && successResponse.getContent() != null) {
				Content content = successResponse.getContent();

				// 优先处理application/json
				MediaType jsonMediaType = content.get("application/json");
				if (jsonMediaType != null && jsonMediaType.getSchema() != null) {
					Schema<?> schema = jsonMediaType.getSchema();
					schemas.put("response", schema);
				}
			}
		}

		return new ApiResponse(schemas);
	}

}