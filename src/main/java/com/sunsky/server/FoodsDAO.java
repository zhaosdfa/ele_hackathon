package com.sunsky.server;

import java.util.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import redis.clients.jedis.*;

public class FoodsDAO {

    // price of food.
    private static Map<Integer, Integer> prices = null;

    public static final String KEY_FOOD_STOCK = "food_stock:";

    public static final String KEY_FOOD_STOCK_INIT = "food_stock_init";

    static {
	Connection conn = null;
	Jedis jedis = null;
	try {
	    conn = DBHelper.getConnection();
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select * from food");
	    prices = new HashMap<Integer, Integer>();
	    jedis = RedisClient.getResource();
	    int init = jedis.incr(KEY_FOOD_STOCK_INIT).intValue();
	    Pipeline pipe = null;
	    if (init == 1) {
		pipe = jedis.pipelined();
	    }
	    while (rs.next()) {
		int id = rs.getInt("id");
		int price = rs.getInt("price");
		int stock = rs.getInt("stock");
		prices.put(new Integer(id), new Integer(price));
		if (pipe != null) {
		    pipe.set(KEY_FOOD_STOCK + id, ""+stock);
		}
	    }
	    if (pipe != null) {
		pipe.sync();
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
	    if (jedis != null) {
		RedisClient.returnResource(jedis);
	    }
	}
    }

    public static int getPrice(int id) {
	return prices.get(id).intValue();
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
	List<Food> list = new ArrayList<Food>();
	Jedis jedis = RedisClient.getResource();
	Pipeline pipe = jedis.pipelined();
	for (Map.Entry<Integer, Integer> entry : prices.entrySet()) {
	    int id = entry.getKey().intValue();
	    int price = entry.getValue().intValue();
	    Food food = new Food();
	    food.setId(id);
	    food.setPrice(price);
	    list.add(food);
	    pipe.get(KEY_FOOD_STOCK + id);
	}
	List<Object> result = pipe.syncAndReturnAll();
	Iterator it = result.iterator();
	for (Food f : list) {
	    int stock = 0;
	    try {
		String t = (String) it.next();
		stock = Integer.parseInt(t);
		f.setStock(stock);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    f.setStock(stock);
	}
	RedisClient.returnResource(jedis);
	return list;
    }

}
