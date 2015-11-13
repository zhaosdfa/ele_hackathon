package com.sunsky.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import org.json.JSONArray;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class LoginHandler implements HttpHandler {

	private static final String ENCODE = "utf-8";

	private Map<String, User> users;
	private Map<String, String> accessTokens;

	public LoginHandler() {
	}

	public void init() {
		// read all users from mysql to memory
		users = new HashMap<String, User>();
		accessTokens = new HashMap<String, String>();
		try {
			Connection conn = DBHelper.getConnection();
			if (conn != null) {
				Statement stmt = conn.createStatement();
				String sql = "select * from user";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					User user = new User();
					user.setId(rs.getInt("id"));
					user.setName(rs.getString("name"));
					user.setPassword(rs.getString("password"));
					user.setAccessToken(Utils.MD5(user.getName()));
					users.put(user.getName(), user);
					accessTokens.put(user.getAccessToken(), user.getName());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ResponseResult _handle(HttpExchange httpExchange) {
		String requestMethod = httpExchange.getRequestMethod();

		int bodyCode = 200;

		String data = ExchangeUtils.getRequestBody(httpExchange, ENCODE);

		if (data == null) {
			JSONObject obj = new JSONObject();
			obj.put("code", "EMPTY_REQUEST");
			obj.put("message", "请求体为空");
			return new ResponseResult(400, obj.toString());
		}

		JSONObject user = null;
		String username = null;
		String password = null;
		try {
			user = new JSONObject(data);
			username = user.getString("username");
			password = user.getString("password");
			if (username == null || password == null) throw new Exception();
		} catch (Exception e) {
			JSONObject obj = new JSONObject();
			obj.put("code", "MALFORMED_JSON");
			obj.put("message", "格式错误");
			return new ResponseResult(400, obj.toString());
		}

		System.out.println("--> " + requestMethod + ", username: " + user.getString("username"));

		int code = 403;
		JSONObject body = new JSONObject();
		if (users.get(username) == null || !users.get(username).getPassword().equals(password)) {
			body.put("code", "USER_AUTH_FAIL");
			body.put("message", "用户名或密码错误");
			code = 403;
		} else {
			User tuser = users.get(username);
			body.put("user_id", tuser.getId());
			body.put("username", username);
			body.put("access_token", tuser.getAccessToken());
			code = 200;
		}

		return new ResponseResult(code, body.toString());
	}

	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			System.out.println("a request arrived");
			ResponseResult result = _handle(httpExchange);
			String responseMsg = result.getBody();
			System.out.println("code: " + result.getCode() + " " + responseMsg);
			httpExchange.sendResponseHeaders(result.getCode(), responseMsg.getBytes().length);
			OutputStream out = httpExchange.getResponseBody();
			out.write(responseMsg.getBytes());
			out.flush();
			httpExchange.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
