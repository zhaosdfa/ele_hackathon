package com.sunsky.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.spi.HttpServerProvider;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class MyHttpServer {

    private HttpServer server;

    /**
     * port: listen port
     * backlog: maxinum number of 
     */
    public void init(int port, int backlog) throws IOException {
	HttpServerProvider provider = HttpServerProvider.provider();
	server = provider.createHttpServer(new InetSocketAddress(port), backlog); 
	server.setExecutor(null);
    }

    public void createContext(String uri, HttpHandler handler) {
	server.createContext(uri, handler);
    }

    public void start() {
	server.start();
    }

}
