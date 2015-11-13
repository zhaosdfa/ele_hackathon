package com.sunsky.server;

import redis.clients.jedis.*;

public class CartDAO {

	private static final String KEY_CART_ID = "cart_id";

	private static final String KEY_CART_CONTENT = "cart_content:";

	private static final String KEY_CART_USR = "cart_user:";

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

	public static int getLength(String cartId) {
		Jedis jedis = RedisClient.getResource();
		int length = jedis.llen(KEY_CART_CONTENT + cartId).intValue();
		RedisClient.returnResource(jedis);
		return length;
	}

	public static void addFood(String cartId, int foodId, int foodCount) {
		Jedis jedis = RedisClient.getResource();
		for (int i = 0; i < foodCount; i++) {
			jedis.lpush(KEY_CART_CONTENT + cartId, ""+foodId);
		}
		RedisClient.returnResource(jedis);
	}

}
