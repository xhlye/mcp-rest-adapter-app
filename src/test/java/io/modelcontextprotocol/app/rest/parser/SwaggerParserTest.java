/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.parser;

import io.modelcontextprotocol.app.rest.parser.ApiEndpoint;
import io.modelcontextprotocol.app.rest.parser.ApiParameter;
import io.modelcontextprotocol.app.rest.parser.SwaggerParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Swagger解析器测试
 */
public class SwaggerParserTest {
    
    private static final String SIMPLE_SWAGGER_JSON = "{\n" +
            "  \"openapi\": \"3.0.0\",\n" +
            "  \"info\": {\n" +
            "    \"title\": \"Test API\",\n" +
            "    \"version\": \"1.0.0\"\n" +
            "  },\n" +
            "  \"paths\": {\n" +
            "    \"/pets\": {\n" +
            "      \"get\": {\n" +
            "        \"summary\": \"List all pets\",\n" +
            "        \"operationId\": \"listPets\",\n" +
            "        \"parameters\": [\n" +
            "          {\n" +
            "            \"name\": \"limit\",\n" +
            "            \"in\": \"query\",\n" +
            "            \"description\": \"How many items to return\",\n" +
            "            \"required\": false,\n" +
            "            \"schema\": {\n" +
            "              \"type\": \"integer\",\n" +
            "              \"format\": \"int32\"\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\n" +
            "            \"description\": \"A list of pets\",\n" +
            "            \"content\": {\n" +
            "              \"application/json\": {\n" +
            "                \"schema\": {\n" +
            "                  \"type\": \"array\",\n" +
            "                  \"items\": {\n" +
            "                    \"type\": \"object\",\n" +
            "                    \"properties\": {\n" +
            "                      \"id\": {\n" +
            "                        \"type\": \"integer\",\n" +
            "                        \"format\": \"int64\"\n" +
            "                      },\n" +
            "                      \"name\": {\n" +
            "                        \"type\": \"string\"\n" +
            "                      }\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"post\": {\n" +
            "        \"summary\": \"Create a pet\",\n" +
            "        \"operationId\": \"createPet\",\n" +
            "        \"requestBody\": {\n" +
            "          \"required\": true,\n" +
            "          \"content\": {\n" +
            "            \"application/json\": {\n" +
            "              \"schema\": {\n" +
            "                \"type\": \"object\",\n" +
            "                \"properties\": {\n" +
            "                  \"name\": {\n" +
            "                    \"type\": \"string\"\n" +
            "                  },\n" +
            "                  \"tag\": {\n" +
            "                    \"type\": \"string\"\n" +
            "                  }\n" +
            "                },\n" +
            "                \"required\": [\"name\"]\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        \"responses\": {\n" +
            "          \"201\": {\n" +
            "            \"description\": \"Created\"\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    },\n" +
            "    \"/pets/{petId}\": {\n" +
            "      \"get\": {\n" +
            "        \"summary\": \"Get a pet by ID\",\n" +
            "        \"operationId\": \"getPet\",\n" +
            "        \"parameters\": [\n" +
            "          {\n" +
            "            \"name\": \"petId\",\n" +
            "            \"in\": \"path\",\n" +
            "            \"required\": true,\n" +
            "            \"description\": \"The ID of the pet to retrieve\",\n" +
            "            \"schema\": {\n" +
            "              \"type\": \"integer\",\n" +
            "              \"format\": \"int64\"\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"responses\": {\n" +
            "          \"200\": {\n" +
            "            \"description\": \"A pet\",\n" +
            "            \"content\": {\n" +
            "              \"application/json\": {\n" +
            "                \"schema\": {\n" +
            "                  \"type\": \"object\",\n" +
            "                  \"properties\": {\n" +
            "                    \"id\": {\n" +
            "                      \"type\": \"integer\",\n" +
            "                      \"format\": \"int64\"\n" +
            "                    },\n" +
            "                    \"name\": {\n" +
            "                      \"type\": \"string\"\n" +
            "                    },\n" +
            "                    \"tag\": {\n" +
            "                      \"type\": \"string\"\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    
    @Test
    void testParseSimpleSwagger() {
        // 给定简单的Swagger JSON
        SwaggerParser parser = new SwaggerParser(SIMPLE_SWAGGER_JSON);
        
        // 执行端点提取
        List<ApiEndpoint> endpoints = parser.extractEndpoints();
        
        // 验证结果
        assertNotNull(endpoints);
        assertEquals(3, endpoints.size());
        
        // 验证GET /pets端点
        ApiEndpoint getPets = endpoints.stream()
                .filter(e -> e.getMethod().equals("GET") && e.getPath().equals("/pets"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(getPets);
        assertEquals("listPets", getPets.getOperationId());
        assertEquals("List all pets", getPets.getSummary());
        assertEquals(1, getPets.getParameters().size());
        
        ApiParameter limitParam = getPets.getParameters().get(0);
        assertEquals("limit", limitParam.getName());
        assertEquals("query", limitParam.getIn());
        assertFalse(limitParam.isRequired());
        assertEquals("How many items to return", limitParam.getDescription());
        
        // 验证POST /pets端点
        ApiEndpoint postPets = endpoints.stream()
                .filter(e -> e.getMethod().equals("POST") && e.getPath().equals("/pets"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(postPets);
        assertEquals("createPet", postPets.getOperationId());
        assertEquals(1, postPets.getParameters().size());  // body参数
        
        ApiParameter bodyParam = postPets.getParameters().get(0);
        assertEquals("body", bodyParam.getName());
        assertEquals("body", bodyParam.getIn());
        assertTrue(bodyParam.isRequired());
        
        // 验证GET /pets/{petId}端点
        ApiEndpoint getPet = endpoints.stream()
                .filter(e -> e.getMethod().equals("GET") && e.getPath().equals("/pets/{petId}"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(getPet);
        assertEquals("getPet", getPet.getOperationId());
        assertEquals(1, getPet.getParameters().size());
        
        ApiParameter petIdParam = getPet.getParameters().get(0);
        assertEquals("petId", petIdParam.getName());
        assertEquals("path", petIdParam.getIn());
        assertTrue(petIdParam.isRequired());
        assertEquals("The ID of the pet to retrieve", petIdParam.getDescription());
    }
} 