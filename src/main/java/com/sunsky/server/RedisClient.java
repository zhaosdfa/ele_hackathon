package com.sunsky.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.SortingParams;



public class RedisClient {

	private static JedisPool jedisPool;

	static {
		JedisPoolConfig config = new JedisPoolConfig(); 
		//config.setMaxActive(20); // deprecated. I didn't know. https://github.com/xetorthio/jedis/issues/849
		config.setMaxIdle(10); 
		config.setMaxTotal(60);
		//config.setMaxWait(1000l); // deprecated
		//config.setTestOnBorrow(false); 
		int port = 6379;
		try {
			port = Integer.parseInt(System.getenv("REDIS_PORT"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		jedisPool = new JedisPool(config, System.getenv("REDIS_HOST"), port);
		Jedis jedis =jedisPool.getResource();
		jedis.flushAll();
		jedisPool.returnResource(jedis);
	}

	public static Jedis getResource() {
		return jedisPool.getResource();
	}

	public static void returnResource(Jedis jedis) {
		jedisPool.returnResource(jedis);
	}

}
