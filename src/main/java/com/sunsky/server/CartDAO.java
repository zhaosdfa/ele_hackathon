package com.sunsky.server;

import redis.clients.jedis.*;
import java.util.*;

public class CartDAO {

	private static final String KEY_CART_ID = "cart_id";

	private static final String KEY_CART_CONTENT = "cart_content:";

	private static final String KEY_CART_USR = "cart_user:";

	private static final String KEY_CART_TOTAL = "cart_total:";

	private static final String KEY_ORDER_CONTENT = "order_content:";

	/**
	  * return cart id
	  */
	public static String createCart(int userId) {
		Jedis jedis = RedisClient.getResource();

		String cartId = jedis.incr(KEY_CART_ID).toString();

		jedis.set(KEY_CART_USR + cartId, ""+userId);

		RedisClient.returnResource(jedis);
		return cartId;
	}

	/**
	  * return null if not exists.
	  */
	public static String getUserId(String cartId) {
		Jedis jedis = RedisClient.getResource();
		String id = jedis.get(KEY_CART_USR + cartId);
		RedisClient.returnResource(jedis);
		return id;
	}

	// TODO
	// Deprecated
	private static int getLength(String cartId) {
		Jedis jedis = RedisClient.getResource();
		int length = jedis.llen(KEY_CART_CONTENT + cartId).intValue();
		RedisClient.returnResource(jedis);
		return length;
	}

	
	// foodCount maybe < 0
	public static boolean addFood(String cartId, int foodId, int foodCount) {
		Jedis jedis = RedisClient.getResource();
		int cur = jedis.incrBy(KEY_CART_TOTAL + cartId, foodCount).intValue();
		boolean flag = true;
		// should judge cnt < 0 ?
		if (cur > 3) {
			// roll back
			jedis.incrBy(KEY_CART_TOTAL + cartId, -foodCount);
			flag = false;
		}
		jedis.hincrBy(KEY_CART_CONTENT + cartId, ""+foodId, foodCount);
		RedisClient.returnResource(jedis);
		return flag;
	}

	// get order details. a map of foods
	public static Map<Integer, Integer> getOrderDetails(String orderId) {
		Jedis jedis = RedisClient.getResource();
		Map<String, String> foods = jedis.hgetAll(KEY_ORDER_CONTENT + orderId);
		Map<Integer, Integer> res = new HashMap();
		for (Map.Entry<String, String> entry : foods.entrySet()) {
			try {
				res.put(Integer.valueOf(entry.getKey()),
						Integer.valueOf(entry.getValue()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		RedisClient.returnResource(jedis);
		return res;
	}

	private static final String KEY_USR_ORDER_ID = "user_order_id:";

	// TODO
	private static String getOrderId(int userId) {
		Jedis jedis = RedisClient.getResource();

		//String orderId = jedis.hset(KEY_USR_ORDER + userId);

		RedisClient.returnResource(jedis);
		return  null;
	}

	
	public enum Result {OK, OUT_OF_LIMIT, OUT_OF_STOCK, FAIL};

	public static Result tryOrder(String cartId, int userId) {
		Jedis jedis = RedisClient.getResource();
		Result res = null;
		do {
			res = _tryOrder(cartId, userId, jedis);
		} while (res == Result.FAIL);
		RedisClient.returnResource(jedis);
		return res;
	}

	// TODO 
	private static Result _tryOrder(String cartId, int userId, Jedis jedis) {
		jedis.watch(KEY_USR_ORDER_ID + userId, KEY_CART_CONTENT + cartId);

		Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);

		boolean outOfStock = false;
		// watch foods
		Map<String, String> newFoods = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : foods.entrySet()) {
			jedis.watch(FoodsDAO.KEY_FOOD_STOCK + entry.getKey());
			try {
				Long newStock = Long.valueOf(jedis.get(FoodsDAO.KEY_FOOD_STOCK + entry.getKey()))
					- Long.valueOf(entry.getValue());
				if (newStock.intValue() < 0) {
					outOfStock = true;
				}
				newFoods.put(entry.getKey(), newStock.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String orderId = jedis.get(KEY_USR_ORDER_ID + userId);

		if (outOfStock || orderId != null) {
			//jedis.unwatch(KEY_USR_ORDER_ID + userId, KEY_CART_CONTENT + cartId);
			//for (Map.Entry<String, String> entry : foods.entrySet()) {
				//jedis.unwatch(FoodsDAO.KEY_FOOD_STOCK + entry.getKey());
			//}
			return outOfStock ? Result.OUT_OF_STOCK : Result.OUT_OF_LIMIT;
		}

		Transaction tx = jedis.multi();

		tx.set(KEY_USR_ORDER_ID + userId, cartId);
		for (Map.Entry<String, String> entry : newFoods.entrySet()) {
			tx.set(FoodsDAO.KEY_FOOD_STOCK + entry.getKey(), entry.getValue());
		}
		tx.hmset(KEY_ORDER_CONTENT + cartId, foods);
		List<Object> list = tx.exec();
		if (list == null || list.size() == 0) {
			System.out.println("transaction: -> FAILED");
			return Result.FAIL;
		}
		for (Object str : list) {
			System.out.println("transaction: -> " + (String) str);
		}
		return Result.OK;
	}

}
