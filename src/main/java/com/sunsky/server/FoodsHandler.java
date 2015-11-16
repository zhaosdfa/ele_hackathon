package com.sunsky.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

public class FoodsHandler implements HttpHandler {


    public void handle(HttpExchange httpExchange) throws IOException {
	try {
	    ResponseResult result = _handle(httpExchange);
	    String body = result.getBody();
	    httpExchange.sendResponseHeaders(result.getCode(), body.getBytes().length);
	    OutputStream out = httpExchange.getResponseBody();
	    out.write(body.getBytes());
	    out.flush();
	    httpExchange.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public ResponseResult _handle(HttpExchange httpExchange) {
	String accessToken = ExchangeUtils.getHeader(httpExchange, "Access-Token");
	if (accessToken == null) {
	    Map<String, String> parameters = ExchangeUtils.getGetParameters(httpExchange);
	    accessToken = parameters.get("access_token"); // GET
	}
	User username = null;
	if (accessToken == null || 
		(username = UserDAO.getUserByAccessToken(accessToken)) == null)
	{
	    JSONObject obj = new JSONObject();
	    obj.put("code", "INVALID_ACCESS_TOKEN");
	    obj.put("message", "无效的令牌");
	    return new ResponseResult(401, obj.toString());
	}
	List<Food> list = FoodsDAO.getAllFoods();
	JSONArray foods = new JSONArray();
	int count = 0;
	for (Food food : list) {
	    JSONObject obj = new JSONObject();
	    obj.put("id", food.getId());
	    obj.put("price", food.getPrice());
	    obj.put("stock", food.getStock());
	    foods.put(count++, obj);
	}
	return new ResponseResult(200, foods.toString());
    }

}
