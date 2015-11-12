package com.sunsky.server;

import java.io.IOException;

public class App {
    public static void main( String[] args ) {

        try {

            MyHttpServer server = new MyHttpServer();
            server.init(8888, 1000);

            server.createContext("/login", new LoginHandler());

            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
