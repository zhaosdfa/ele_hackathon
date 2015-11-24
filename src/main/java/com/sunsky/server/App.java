package com.sunsky.server;

import java.io.IOException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.nio.*;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class App {

    public static void main( String[] args ) {

	try {

	    System.out.println("loading...");
	    UserDAO dao = new UserDAO();
	    FoodsDAO fd = new FoodsDAO();
	    LuaScript ls = new LuaScript();
	    System.out.println("loaded");


	    int port = 8080;
	    try {
		port = Integer.parseInt(System.getenv("APP_PORT"));
	    } catch (Exception e) {
		e.printStackTrace();
	    }

	    // Setup Threadpool
	    QueuedThreadPool threadPool = new QueuedThreadPool(12);
//	    threadPool.setMaxThreads(500);
//	    threadPool.setMinThreads(100);

	    // HTTP Configuration
	    HttpConfiguration http_config = new HttpConfiguration();
	    //http_config.setSecureScheme("https");
	    //http_config.setSecurePort(8443);
	    http_config.setOutputBufferSize(32768);
	    http_config.setRequestHeaderSize(1024);
	    http_config.setResponseHeaderSize(1024);
	    http_config.setSendServerVersion(false);
	    http_config.setSendDateHeader(false);
	    // httpConfig.addCustomizer(new ForwardedRequestCustomizer());


//	    Server server = new Server(port);
	    Server server = new Server(threadPool);

	    // Extra options
	    server.setDumpAfterStart(false);
	    server.setDumpBeforeStop(false);

	    //SelectChannelConnector connector = new SelectChannelConnector();
//	    NetworkTrafficSelectChannelConnector connector = new NetworkTrafficSelectChannelConnector(server);
//	    connector.setPort(port);
//	    connector.setHost(System.getenv("APP_HOST"));

	    // === jetty-http.xml ===
	    ServerConnector http = new ServerConnector(server,
		    new HttpConnectionFactory(http_config));
	    http.setPort(port);
	    http.setIdleTimeout(10000);

	    server.addConnector(http);


	    ServletHandler handler = new ServletHandler();
	    server.setHandler(handler);

	    handler.addServletWithMapping(LoginServlet.class, "/login");
	    handler.addServletWithMapping(FoodsServlet.class, "/foods");
	    handler.addServletWithMapping(CartServlet.class, "/carts");
	    handler.addServletWithMapping(CartServlet.class, "/carts/*");
	    handler.addServletWithMapping(OrderServlet.class, "/orders");
	    handler.addServletWithMapping(AdminServlet.class, "/admin/orders");

	    server.start();
	    server.join();

	    System.out.println("server started");

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }
}
