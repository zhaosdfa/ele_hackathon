package com.sunsky.server;

import java.io.IOException;
//import com.sun.net.httpserver.HttpHandler;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

public class App {

    public static void main( String[] args ) {

	try {

	    System.out.println("loading...");
	    UserDAO dao = new UserDAO();
	    FoodsDAO fd = new FoodsDAO();
	    System.out.println("loaded");


	    int port = 8080;
	    try {
		port = Integer.parseInt(System.getenv("APP_PORT"));
	    } catch (Exception e) {
		e.printStackTrace();
	    }

	    Server server = new Server(port);

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
