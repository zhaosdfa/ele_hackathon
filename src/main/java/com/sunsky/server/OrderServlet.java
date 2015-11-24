package com.sunsky.server;

import java.io.IOException;
import java.io.PrintWriter;
 
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

public class OrderServlet extends HttpServlet {

    public void service(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	try {

	    res.setCharacterEncoding("utf-8");

	    ResponseResult result = _handle(req, res);
	    String responseMsg = result.getBody();

	    Utils.println("code: " + result.getCode() + " " + responseMsg);

	    res.setStatus(result.getCode());
	    res.getWriter().println(responseMsg);

	} catch (Exception e) {
	    Utils.print(e);
	}
    }

    public ResponseResult _handle(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	String body = Utils.readBody(req);

	String accessToken = req.getHeader("Access-Token");
	if (accessToken == null) {
	    accessToken = req.getParameter("access_token");
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

	String method = req.getMethod();

	if (method.equals("POST")) {
	    return order(body, user);
	} else {
	    return show(user);
	}
    }


    private ResponseResult order(String requestBody, User user) {
	String data = requestBody;
	if (data == null) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "EMPTY_REQUEST");
	    obj.put("message", "请求体为空");
	    return new ResponseResult(400, obj.toString());
	}

	String cartId = null;
	try {
	    Utils.println("data: " + data);
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

    private ResponseResult show(User user) {
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
