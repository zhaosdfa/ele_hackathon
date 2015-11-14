package com.sunsky.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

public class CartHandler implements HttpHandler {

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
		User user = null;
		if (accessToken == null ||
				(user = UserDAO.getUserByAccessToken(accessToken)) == null)
		{
			JSONObject obj = new JSONObject();
			obj.put("code", "INVALID_ACCESS_TOKEN");
			obj.put("message", "无效的令牌");
			return new ResponseResult(401, obj.toString());
		}

		String requestMethod = httpExchange.getRequestMethod();
		if (requestMethod.equals("PATCH")) {
			return addFood(httpExchange, user);
		} else {
			return createCart(httpExchange, user);
		}
	}

	public ResponseResult createCart(HttpExchange httpExchange, User user) {
		String cartId = CartDAO.createCart(user.getId());
		JSONObject obj = new JSONObject();
		obj.put("cart_id", cartId);
		return new ResponseResult(200, obj.toString());
	}

	public ResponseResult addFood(HttpExchange httpExchange, User user) {
		String uri = httpExchange.getRequestURI().toString();
		String[] parts = uri.split("\\?");
		String[] tmp = parts[0].split("/");
		int sz = tmp.length;
		String cartId = null;
		if (sz > 0) {
			cartId = tmp[sz-1];
		}

		String str = CartDAO.getUserId(cartId);
		int userId = -1;
		boolean exist = false;
		try {
			userId = Integer.parseInt(str);
			exist = true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!exist) {
			JSONObject obj = new JSONObject();
			obj.put("code", "CART_NOT_FOUND");
			obj.put("message", "篮子不存在");
			return new ResponseResult(404, obj.toString());
		}

		if (userId != user.getId()) {
			JSONObject obj = new JSONObject();
			obj.put("code", "NOT_AUTHORIZED_TO_ACCESS_CART");
			obj.put("message", "无权限访问指定的篮子");
			return new ResponseResult(401, obj.toString());
		}


		int foodId = -1;
		int foodCount = -1;
		try {
			JSONObject body = new JSONObject(ExchangeUtils.getRequestBody(httpExchange, "utf-8"));
			foodId = body.getInt("food_id");
			foodCount = body.getInt("count");
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!FoodsDAO.exists(foodId)) {
			JSONObject obj = new JSONObject();
			obj.put("code", "FOOD_NOT_FOUND");
			obj.put("message", "食物不存在");
			return new ResponseResult(404, obj.toString());
		}

		if (foodCount > 3 || !CartDAO.addFood(cartId, foodId, foodCount)) {
			JSONObject obj = new JSONObject();
			obj.put("code", "FOOD_OUT_OF_LIMIT");
			obj.put("message", "篮子中食物数量超过了三个");
			return new ResponseResult(403, obj.toString());
		}

		return new ResponseResult(204, "");
	}

}
