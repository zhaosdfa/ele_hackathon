package com.sunsky.server;

import java.io.IOException;
import com.sun.net.httpserver.HttpHandler;

public class App {
    public static void main( String[] args ) {

        try {

            MyHttpServer server = new MyHttpServer();
            server.init(8080, 1000);

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

            server.start();

	    System.out.println("server started");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
