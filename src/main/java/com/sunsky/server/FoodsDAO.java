package com.sunsky.server;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class FoodsDAO {

	// price of food.
	private static Map<Integer, Integer> prices = null;

	static {
		Connection conn = null;
		try {
			conn = DBHelper.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select id, price from food");
			prices = new HashMap<Integer, Integer>();
			while (rs.next()) {
				int id = rs.getInt("id");
				int price = rs.getInt("price");
				prices.put(new Integer(id), price);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static int getPrice(int id) {
		return prices.get(id);
	}

	public static int getStack(int id) {
		return 0;
	}

	public static boolean exists(int id) {
		return prices.get(id) != null;
	}

	private static void setStack(int id) {
	}
	
	public static List<Food> getAllFoods() {
		Connection conn = null;
		List<Food> list = new ArrayList<Food>();
		try {
			conn = DBHelper.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from food");
			while (rs.next()) {
				Food food = new Food();
				food.setId(rs.getInt("id"));
				food.setPrice(rs.getInt("price"));
				food.setStock(rs.getInt("stock"));
				list.add(food);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

}
