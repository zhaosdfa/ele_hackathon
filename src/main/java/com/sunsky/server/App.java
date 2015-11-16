package com.sunsky.server;

import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;

public class App {
    public static void main( String[] args ) {

	try {

	    System.out.println("loading...");
	    UserDAO dao = new UserDAO();
	    FoodsDAO fd = new FoodsDAO();
	    System.out.println("loaded");

	    MyHttpServer server = new MyHttpServer();
	    int port = 8080;
	    try {
		port = Integer.parseInt(System.getenv("APP_PORT"));
	    } catch (Exception e) {
		e.printStackTrace();
	    }

	    server.init(port, 1000);

	    // add login api
	    LoginHandler login = new LoginHandler();
	    login.init();
	    server.createContext("/login", login);

	    // add foods api
	    FoodsHandler foods = new FoodsHandler();
	    server.createContext("/foods", foods);

	    // add cart api
	    CartHandler cart = new CartHandler();
	    server.createContext("/carts", cart);

	    OrderHandler order = new OrderHandler();
	    server.createContext("/orders", order);

	    AdminHandler admin = new AdminHandler();
	    server.createContext("/admin/orders", admin);

	    server.start();

	    System.out.println("server started");

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }
}
