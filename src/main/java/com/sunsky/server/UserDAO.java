package com.sunsky.server;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class UserDAO {

	private static Map<String, User> users;
	private static Map<String, String> accessTokens;

	static {
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

	public static User get(String name) {
		return users.get(name);
	}

//	public static void addAccessToken(String name) {
//		User user = users.get(name);
//		if (user == null) return ;
//		accessTokens.put(user.getAccessToken(), name);
//	}

	public static User getUserByAccessToken(String accessToken) {
		String name = accessTokens.get(accessToken);
		if (name == null) return null;
		return users.get(name);
	}

}
