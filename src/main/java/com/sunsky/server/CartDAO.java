package com.sunsky.server;

import redis.clients.jedis.*;
import java.util.*;
import org.json.*;

public class CartDAO {

	private static final String KEY_CART_ID = "cart_id";

	private static final String KEY_CART_CONTENT = "cart_content:";

	private static final String KEY_CART_USR = "cart_user:";

	private static final String KEY_CART_TOTAL = "cart_total:";

	private static final String KEY_ORDER_CONTENT = "order_content:";

	private static final String KEY_ALL_ORDERS = "all_orders";

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
	// this function may have race condition , may be need use transaction
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
		// should flag == true
		if (flag == true){
			jedis.hincrBy(KEY_CART_CONTENT + cartId, ""+foodId, foodCount);
		}
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
//		Result res = _tryOrderV2(cartId, userId, jedis);
//		if (res != Result.OK) res = _tryOrderV2(cartId, userId, jedis);
		Result res = null;
		int cnt = 0;
		do {
			res = _tryOrder(cartId, userId, jedis);
		} while (res == Result.FAIL && cnt++ <= 10);
		RedisClient.returnResource(jedis);
		return res;
	}

	private static final String KEY_USR_ORDER_NUM = "user_order_num:";
	private static Result _tryOrderV2(String cartId, int userId, Jedis jedis) {
		int orderNum = jedis.incr(KEY_USR_ORDER_NUM + userId).intValue();
		if (orderNum > 1) {
			return Result.OUT_OF_LIMIT;
		}

		Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);
		JSONObject ord = new JSONObject();
		ord.put("id", cartId);
		ord.put("user_id", userId);

		int total = 0;
		JSONArray items = new JSONArray();
		int itemsIndex = 0;

		boolean outOfStock = false;
		for (Map.Entry<String, String> entry : foods.entrySet()) {
			try {
				int fid = Integer.parseInt(entry.getKey());
				int cnt = Integer.parseInt(entry.getValue());
				if (cnt > 0) {
					int t = jedis.incrBy(FoodsDAO.KEY_FOOD_STOCK + entry.getKey(), cnt).intValue();
					if (t < 0) {
						outOfStock = true;
						continue ;
					}
					JSONObject it = new JSONObject();
					it.put("food_id", fid);
					it.put("count", cnt);
					items.put(itemsIndex++, it);

					total += FoodsDAO.getPrice(fid) * cnt;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (outOfStock) {
			for (Map.Entry<String, String> entry : foods.entrySet()) {
				try {
					int fid = Integer.parseInt(entry.getKey());
					int cnt = Integer.parseInt(entry.getValue());
					if (cnt > 0) {
						int t = jedis.incrBy(FoodsDAO.KEY_FOOD_STOCK + entry.getKey(), -cnt).intValue();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			return Result.OUT_OF_STOCK;
		}

		jedis.set(KEY_USR_ORDER_ID + userId, cartId);

		ord.put("total", total);
		ord.put("items", items);

		jedis.lpush(KEY_ALL_ORDERS, ord.toString());

		return Result.OK;
	}

	// TODO 
	private static Result _tryOrder(String cartId, int userId, Jedis jedis) {
		jedis.watch(KEY_USR_ORDER_ID + userId, KEY_CART_CONTENT + cartId);

		Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);

		JSONObject ord = new JSONObject();
		ord.put("id", cartId);
		ord.put("user_id", userId);

		int total = 0;

		JSONArray items = new JSONArray();
		int itemsIndex = 0;

		boolean outOfStock = false;
		// watch foods
		Map<String, String> newFoods = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : foods.entrySet()) {
			jedis.watch(FoodsDAO.KEY_FOOD_STOCK + entry.getKey());
			try {
				int fid = Integer.parseInt(entry.getKey());
				int cnt = Integer.parseInt(entry.getValue());
				if (cnt > 0) {
					JSONObject it = new JSONObject();
					it.put("food_id", fid);
					it.put("count", cnt);
					items.put(itemsIndex++, it);
					total += FoodsDAO.getPrice(fid) * cnt;
					Long newStock = Long.valueOf(jedis.get(FoodsDAO.KEY_FOOD_STOCK + entry.getKey()))
						- Long.valueOf(entry.getValue());
					if (newStock.intValue() < 0) {
						outOfStock = true;
					}
					newFoods.put(entry.getKey(), newStock.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ord.put("total", total);
		ord.put("items", items);

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
		tx.lpush(KEY_ALL_ORDERS, ord.toString());
		List<Object> list = tx.exec();
		if (list == null || list.size() == 0) {
			System.out.println("transaction: -> FAILED");
			return Result.FAIL;
		}
		for (Object str : list) {
			System.out.println("transaction: -> " + str);
		}
		return Result.OK;
	}

	public static String getOrders() {
		Jedis jedis = RedisClient.getResource();
		List<String> list = jedis.lrange(KEY_ALL_ORDERS, 0, -1);
//		if (list == null || list.size() == 0) {
//			return "";
//		}
		System.out.println("list size: " + list.size());
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		boolean first = true;
		for (String s : list) {
			if (!first) builder.append(",");
			first = false;
			builder.append(s);
		}
		builder.append("]");
		RedisClient.returnResource(jedis);
		return builder.toString();
	}

}
