package com.sunsky.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;


public class OrderHandler implements HttpHandler {

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

    private ResponseResult _handle(HttpExchange httpExchange) {
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
	if (requestMethod.equals("POST")) {
	    return order(httpExchange, user);
	} else {
	    return show(httpExchange, user);
	}
    }

    private ResponseResult order(HttpExchange httpExchange, User user) {
	String data = ExchangeUtils.getRequestBody(httpExchange, "utf-8");
	if (data == null) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "EMPTY_REQUEST");
	    obj.put("message", "请求体为空");
	    return new ResponseResult(400, obj.toString());
	}

	String cartId = null;
	try {
	    System.out.println("data: " + data);
	    cartId = new JSONObject(data).getString("cart_id");
	} catch (Exception e) {
	    e.printStackTrace();
	}

	if (cartId == null) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "MALFORMED_JSON");
	    obj.put("message", "格式错误");
	    return new ResponseResult(400, obj.toString());
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

	CartDAO.Result res = CartDAO.tryOrder(cartId, userId);

	if (res == CartDAO.Result.OUT_OF_STOCK) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "FOOD_OUT_OF_STOCK");
	    obj.put("message", "食物库存不足");
	    return new ResponseResult(403, obj.toString());
	} else if (res == CartDAO.Result.OUT_OF_LIMIT) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "ORDER_OUT_OF_LIMIT");
	    obj.put("message", "每个用户只能下一单");
	    return new ResponseResult(403, obj.toString());
	}

	JSONObject obj = new JSONObject();
	obj.put("id", cartId);
	return new ResponseResult(200, obj.toString());
    }

    private ResponseResult show(HttpExchange httpExchange, User user) {
	String orderId = UserDAO.getOrderId(user.getId());
	JSONArray arr = new JSONArray();
	int arrIndex = 0;
	if (orderId == null) {
	    return new ResponseResult(200, "[]".toString());
	}
	JSONObject obj = new JSONObject();
	obj.put("id", orderId);

	Map<Integer, Integer> counts = CartDAO.getOrderDetails(orderId);

	int totalPrice = 0;
	JSONArray items = new JSONArray();
	int index = 0;
	for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
	    int foodId = entry.getKey().intValue();
	    int cnt = entry.getValue().intValue();
	    int pri = FoodsDAO.getPrice(foodId);
	    totalPrice += pri * cnt;
	    JSONObject fd = new JSONObject();
	    fd.put("food_id", foodId);
	    fd.put("count", cnt);
	    items.put(index++, fd);
	}

	obj.put("items", items);
	obj.put("total", totalPrice);

	arr.put(arrIndex++, obj);

	return new ResponseResult(200, arr.toString());
    }

}
