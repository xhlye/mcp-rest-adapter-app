/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.generator;

import io.modelcontextprotocol.app.rest.parser.ApiEndpoint;
import io.modelcontextprotocol.app.rest.parser.ApiParameter;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据API端点生成MCP工具定义
 */
public class McsToolGenerator {

	private static final Logger logger = LoggerFactory.getLogger(McsToolGenerator.class);

	private final String toolNamePrefix;

	public McsToolGenerator() {
		this("");
	}

	public McsToolGenerator(String toolNamePrefix) {
		this.toolNamePrefix = toolNamePrefix != null ? toolNamePrefix : "";
	}

	/**
	 * 将API端点转换为MCP工具
	 */
	public Tool generateTool(ApiEndpoint endpoint) {
		String name = generateToolName(endpoint);
		String description = generateToolDescription(endpoint);
		JsonSchema schema = generateInputSchema(endpoint);

		return new Tool(name, description, schema);
	}

	/**
	 * 为多个API端点生成MCP工具列表
	 */
	public List<Tool> generateTools(List<ApiEndpoint> endpoints) {
		List<Tool> tools = new ArrayList<>();

		for (ApiEndpoint endpoint : endpoints) {
			try {
				tools.add(generateTool(endpoint));
			}
			catch (Exception e) {
				logger.error("无法为端点生成工具: {} {}, 原因: {}", endpoint.getMethod(), endpoint.getPath(), e.getMessage());
				logger.debug("工具生成失败的详细信息", e);
				// 继续处理其他端点
			}
		}

		return tools;
	}

	/**
	 * 生成工具名称
	 */
	public String generateToolName(ApiEndpoint endpoint) {
		if (endpoint.getOperationId() != null && !endpoint.getOperationId().isEmpty()) {
			return toolNamePrefix + endpoint.getOperationId();
		}

		// 根据路径和方法生成名称
		String path = endpoint.getPath()
			.replaceAll("\\{([^}]*)\\}", "$1") // 将路径参数 {param} 转换为 param
			.replaceAll("[^a-zA-Z0-9_]", "_") // 将非字母数字字符转换为下划线
			.replaceAll("_+", "_") // 将多个下划线转换为单个下划线
			.replaceAll("^_|_$", ""); // 删除开头和结尾的下划线

		return toolNamePrefix + endpoint.getMethod().toLowerCase() + "_" + path;
	}

	/**
	 * 生成工具描述
	 */
	private String generateToolDescription(ApiEndpoint endpoint) {
		StringBuilder description = new StringBuilder();

		// 添加端点基本信息
		description.append(endpoint.getMethod()).append(" ").append(endpoint.getPath());

		// 添加摘要或描述
		if (endpoint.getSummary() != null && !endpoint.getSummary().isEmpty()) {
			description.append("\n\n").append(endpoint.getSummary());
		}
		else if (endpoint.getDescription() != null && !endpoint.getDescription().isEmpty()) {
			description.append("\n\n").append(endpoint.getDescription());
		}

		// 添加参数信息
		if (!endpoint.getParameters().isEmpty()) {
			description.append("\n\nParameters:");
			for (ApiParameter parameter : endpoint.getParameters()) {
				description.append("\n- ")
					.append(parameter.getName())
					.append(" (")
					.append(parameter.getIn())
					.append(parameter.isRequired() ? ", required" : "")
					.append(")");

				if (parameter.getDescription() != null && !parameter.getDescription().isEmpty()) {
					description.append(": ").append(parameter.getDescription());
				}
			}
		}

		return description.toString();
	}

	/**
	 * 生成输入Schema
	 */
	private JsonSchema generateInputSchema(ApiEndpoint endpoint) {
		Map<String, Object> properties = new HashMap<>();
		List<String> required = new ArrayList<>();

		for (ApiParameter parameter : endpoint.getParameters()) {
			Map<String, Object> paramProperties = new HashMap<>();

			// 设置参数类型
			paramProperties.put("type", convertType(parameter.getSchema()));

			// 设置参数描述
			if (parameter.getDescription() != null) {
				paramProperties.put("description", parameter.getDescription());
			}

			// 处理数组类型
			if (parameter.getSchema() instanceof ArraySchema) {
				paramProperties.put("items", convertArrayItems(((ArraySchema) parameter.getSchema()).getItems()));
			}

			// 处理对象类型
			if (parameter.getSchema() instanceof ObjectSchema) {
				processObjectSchema((ObjectSchema) parameter.getSchema(), paramProperties);
			}

			// 添加枚举值
			if (parameter.getSchema().getEnum() != null && !parameter.getSchema().getEnum().isEmpty()) {
				paramProperties.put("enum", parameter.getSchema().getEnum());
			}

			properties.put(parameter.getName(), paramProperties);

			if (parameter.isRequired()) {
				required.add(parameter.getName());
			}
		}

		// 使用最新的JsonSchema构造方法，包含额外的属性
		boolean allowAdditionalProperties = false; // 默认不允许额外属性

		// 创建一个新的JsonSchema实例，添加了$defs和definitions支持
		return new JsonSchema("object", // 类型
				properties, // 属性
				required, // 必需字段
				allowAdditionalProperties, // 是否允许额外属性
				null, // $defs（在当前场景下不需要，但保留扩展性）
				null // definitions（在当前场景下不需要，但保留扩展性）
		);
	}

	/**
	 * 处理对象类型Schema
	 */
	private void processObjectSchema(ObjectSchema schema, Map<String, Object> paramProperties) {
		if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
			Map<String, Object> objectProperties = new HashMap<>();

			for (Map.Entry<String, Schema> prop : schema.getProperties().entrySet()) {
				Map<String, Object> propMap = new HashMap<>();
				propMap.put("type", convertType(prop.getValue()));

				if (prop.getValue().getDescription() != null) {
					propMap.put("description", prop.getValue().getDescription());
				}

				// 处理嵌套对象
				if (prop.getValue() instanceof ObjectSchema) {
					Map<String, Object> nestedProps = new HashMap<>();
					processObjectSchema((ObjectSchema) prop.getValue(), nestedProps);

					// 将嵌套对象的属性添加到当前属性
					for (Map.Entry<String, Object> entry : nestedProps.entrySet()) {
						propMap.put(entry.getKey(), entry.getValue());
					}
				}

				// 处理嵌套数组
				if (prop.getValue() instanceof ArraySchema) {
					propMap.put("items", convertArrayItems(((ArraySchema) prop.getValue()).getItems()));
				}

				// 处理枚举值
				if (prop.getValue().getEnum() != null && !prop.getValue().getEnum().isEmpty()) {
					propMap.put("enum", prop.getValue().getEnum());
				}

				objectProperties.put(prop.getKey(), propMap);
			}

			paramProperties.put("properties", objectProperties);

			if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
				paramProperties.put("required", schema.getRequired());
			}

			// 处理additionalProperties属性
			if (schema.getAdditionalProperties() != null) {
				// 如果additionalProperties是布尔值
				if (schema.getAdditionalProperties() instanceof Boolean) {
					paramProperties.put("additionalProperties", schema.getAdditionalProperties());
				}
				// 如果additionalProperties是Schema
				else if (schema.getAdditionalProperties() instanceof Schema) {
					Map<String, Object> additionalPropsSchema = new HashMap<>();
					Schema<?> additionalSchema = (Schema<?>) schema.getAdditionalProperties();
					additionalPropsSchema.put("type", convertType(additionalSchema));

					if (additionalSchema.getDescription() != null) {
						additionalPropsSchema.put("description", additionalSchema.getDescription());
					}

					paramProperties.put("additionalProperties", additionalPropsSchema);
				}
			}
		}
	}

	/**
	 * 处理数组项
	 */
	private Map<String, Object> convertArrayItems(Schema<?> itemsSchema) {
		Map<String, Object> items = new HashMap<>();
		items.put("type", convertType(itemsSchema));

		if (itemsSchema.getDescription() != null) {
			items.put("description", itemsSchema.getDescription());
		}

		// 处理数组元素为对象的情况
		if (itemsSchema instanceof ObjectSchema) {
			Map<String, Object> objectProps = new HashMap<>();
			processObjectSchema((ObjectSchema) itemsSchema, objectProps);

			for (Map.Entry<String, Object> entry : objectProps.entrySet()) {
				items.put(entry.getKey(), entry.getValue());
			}
		}

		return items;
	}

	/**
	 * 转换Swagger类型到JSON Schema类型
	 */
	private String convertType(Schema<?> schema) {
		if (schema == null) {
			return "string";
		}

		if (schema instanceof ArraySchema) {
			return "array";
		}

		if (schema.getType() == null) {
			// 对象类型可能没有显式类型
			if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
				return "object";
			}
			return "string"; // 默认为字符串
		}

		switch (schema.getType()) {
			case "integer":
				return "integer";
			case "number":
				return "number";
			case "boolean":
				return "boolean";
			case "array":
				return "array";
			case "object":
				return "object";
			default:
				return "string";
		}
	}

}