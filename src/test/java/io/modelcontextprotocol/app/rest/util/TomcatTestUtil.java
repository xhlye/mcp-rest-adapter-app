/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.app.rest.util;

import jakarta.servlet.Servlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

/**
 * 用于测试的Tomcat实用工具类
 */
public class TomcatTestUtil {

	/**
	 * 创建一个用于测试的嵌入式Tomcat服务器
	 * @param contextPath 上下文路径
	 * @param port 端口号
	 * @param servlet MCP服务器传输提供者
	 * @return 配置好的Tomcat实例
	 */
	public static Tomcat createTomcatServer(String contextPath, int port, Servlet servlet) {

		var tomcat = new Tomcat();
		tomcat.setPort(port);

		String baseDir = System.getProperty("java.io.tmpdir");
		tomcat.setBaseDir(baseDir);

		// Context context = tomcat.addContext("", baseDir);
		Context context = tomcat.addContext(contextPath, baseDir);

		// Add transport servlet to Tomcat
		org.apache.catalina.Wrapper wrapper = context.createWrapper();
		wrapper.setName("mcpServlet");
		wrapper.setServlet(servlet);
		wrapper.setLoadOnStartup(1);
		wrapper.setAsyncSupported(true);
		context.addChild(wrapper);
		context.addServletMappingDecoded("/*", "mcpServlet");

		var connector = tomcat.getConnector();
		connector.setAsyncTimeout(3000);

		return tomcat;
	}

}