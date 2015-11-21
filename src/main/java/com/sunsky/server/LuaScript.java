package com.sunsky.server;

import java.util.*;
import redis.clients.jedis.*;

public class LuaScript {
	public static String sha1 = null;
	public static String sha2 = null;
	public static String sha3 = null;
	static {
		Jedis jedis = null;
		String Script1 = "local ok = 1; local key_food_stock = 'food_stock:'; " +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end;" +
			"if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); return 3; else return 1; end;";
		String Script2 = "local ok = 1; local key_food_stock = 'food_stock:'; " +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end;" +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[2])) < tonumber(ARGV[2]) then ok = 0; end;" +
			"if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); " +
			"redis.call('INCRBY',key_food_stock..KEYS[2],-tonumber(ARGV[2])); " +
			"return 3; else return 1; end;";
		String Script3 = "local ok = 1; local key_food_stock = 'food_stock:'; " +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end;" +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[2])) < tonumber(ARGV[2]) then ok = 0; end;" +
			"if tonumber(redis.call('GET',key_food_stock..KEYS[3])) < tonumber(ARGV[3]) then ok = 0; end;" +
			"if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); " +
			"redis.call('INCRBY',key_food_stock..KEYS[2],-tonumber(ARGV[2])); " +
			"redis.call('INCRBY',key_food_stock..KEYS[3],-tonumber(ARGV[3])); " +
			"return 3; else return 1; end;";
		try {
			jedis = RedisClient.getResource();
			sha1 = jedis.scriptLoad(Script1);
			sha2 = jedis.scriptLoad(Script2);
			sha3 = jedis.scriptLoad(Script3);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (jedis != null) {
				RedisClient.returnResource(jedis);
			}
		}
	}
}
