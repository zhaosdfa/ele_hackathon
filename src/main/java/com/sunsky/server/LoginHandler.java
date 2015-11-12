package com.sunsky.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONArray;

public class LoginHandler implements HttpHandler {

    private static final String ENCODE = "utf-8";

    private void _handle(HttpExchange httpExchange) throws IOException {
        String requestMethod = httpExchange.getRequestMethod();

        /*
        Headers headers = httpExchange.getRequestHeaders();
        List<String> access = headers.get("Access-Token");
        String accessToken = null;
        if (access != null && access.size() > 0) {
            accessToken = access.get(0);
        }
        */


        int responseCode = 200;

        String data = ExchangeUtils.getRequestBody(httpExchange, ENCODE);

        JSONObject user = new JSONObject(data);

        System.out.println("--> " + requestMethod + ", " + httpExchange.getRequestURI().toString()
                + ", data: " + user.getString("username"));

        String username = user.getString("username");
        String password = user.getString("password");

        JSONObject response = new JSONObject();


        if (username == null || password == null || username.equals("wa")) {
            response.put("code", "USER_AUTH_FAIL");
            response.put("message", "用户名或密码错误");
            responseCode = 403;
        } else {
            response.put("user_id", "1");
            response.put("username", username);
            response.put("access_token", "abcdabcdlsjdflsjdflsjdfljslfjkjs");
            responseCode = 200;
        }

        String responseMsg = response.toString();
        httpExchange.sendResponseHeaders(responseCode, responseMsg.getBytes().length);
        OutputStream out = httpExchange.getResponseBody();
        out.write(responseMsg.getBytes());
        out.flush();
        httpExchange.close();

    }

    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            _handle(httpExchange);
        } catch (Exception e) {
            System.out.println(e);
            JSONObject response = new JSONObject();
            response.put("code", "MALFORMED_JSON");
            response.put("message", "格式错误");
            String responseMsg = response.toString();
            httpExchange.sendResponseHeaders(400, responseMsg.getBytes().length);
            OutputStream out = httpExchange.getResponseBody();
            out.write(responseMsg.getBytes());
            out.flush();
            httpExchange.close();
        }
    }

}
