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

public class FoodsServlet extends HttpServlet {

    public void service(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	try {

	    res.setCharacterEncoding("utf-8");

	    ResponseResult result = _handle(req, res);
	    String responseMsg = result.getBody();

	    //Utils.println("code: " + result.getCode() + " " + responseMsg);

	    res.setStatus(result.getCode());
	    res.getWriter().println(responseMsg);

	} catch (Exception e) {
	    Utils.print(e);
	}
    }

    static {
	flush();
    }

    private static String cachedStock;
    private static int cachedTimes = 0;
    private static void flush () {
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
	cachedStock = foods.toString();
    }

    public ResponseResult _handle(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException
    {
	String accessToken = req.getHeader("Access-Token");
	if (accessToken == null) {
	    accessToken = req.getParameter("access_token");
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
	if (++cachedTimes % 2000 == 0) {
	    flush();
	}
	return new ResponseResult(200, cachedStock);
    }


}
