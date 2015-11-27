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

public class CartServlet extends HttpServlet {

    private static long total_time = 0;
    private static long total_num = 0;
    private static long dao_time = 0;
    private static long dao_num = 0;

    public void service(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
//	boolean addFoods = req.getMethod().equals("PATCH");
//	long start = 0;
//	if (addFoods) {
//	    start = System.currentTimeMillis();
//	}
	try {
	    res.setCharacterEncoding("utf-8");

	    ResponseResult result = _handle(req, res);
	    String responseMsg = result.getBody();

	    res.setStatus(result.getCode());
	    if (responseMsg != null && !responseMsg.equals("")) {
		res.getWriter().println(responseMsg);
	    }
	    //res.getWriter().close();

	    //Utils.println("code: " + result.getCode() + " " + responseMsg);
	} catch (Exception e) {
	    Utils.print(e);
	}
//	if (addFoods) {
//	    long end = System.currentTimeMillis();
//	    total_time += end - start;
//	    total_num ++;
//	    if (total_num % 100 == 0) {
//		double total_avg = total_time / (float) total_num;
//		double dao_avg = dao_time / (float) dao_num;
//		System.out.println("total: " + total_avg + ", dao: " + dao_avg);
//	    }
//	}
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

	    ResponseResult ret = null;
	    if (method.equals("PATCH")) {
//		long start = System.currentTimeMillis();
		ret = addFood(req, user, body);
//		long end = System.currentTimeMillis();
//		dao_time += end - start;
//		dao_num++;
	    } else {
		ret = createCart(user);
	    }
	    return ret;
	}

    public ResponseResult createCart(User user) {
	String cartId = CartDAO.createCart(user.getId());
	JSONObject obj = new JSONObject();
	obj.put("cart_id", cartId);
	return new ResponseResult(200, obj.toString());
    }

    public ResponseResult addFood(HttpServletRequest req, User user, String requestBody) {
	String uri = req.getRequestURI();
	int index = uri.indexOf("?");
	if (index < 0) {
	    index = uri.length();
	}
	String cartId = uri.substring(7, index);

	String str = CartDAO.getUserId(cartId);
	int userId = -1;
	boolean exist = false;
	try {
	    userId = Integer.parseInt(str);
	    exist = true;
	} catch (Exception e) {
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
	    JSONObject body = new JSONObject(requestBody);
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

	if (foodCount > 3) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "FOOD_OUT_OF_LIMIT");
	    obj.put("message", "篮子中食物数量超过了三个");
	    return new ResponseResult(403, obj.toString());
	}

	//long start = System.currentTimeMillis();
	boolean ret = CartDAO.addFood(cartId, foodId, foodCount);
	//long end = System.currentTimeMillis();

	//dao_time += end - start;
	//dao_num++;

	if (!ret) {
	    JSONObject obj = new JSONObject();
	    obj.put("code", "FOOD_OUT_OF_LIMIT");
	    obj.put("message", "篮子中食物数量超过了三个");
	    return new ResponseResult(403, obj.toString());
	}

	return new ResponseResult(204, "");
    }


}
