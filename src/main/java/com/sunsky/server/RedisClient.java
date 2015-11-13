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

	private static Jedis jedis;
	private static JedisPool jedisPool;

	static {
		JedisPoolConfig config = new JedisPoolConfig(); 
		//config.setMaxActive(20); // deprecated. I didn't know. https://github.com/xetorthio/jedis/issues/849
		config.setMaxIdle(5); 
		//config.setMaxWait(1000l); // deprecated
		//config.setTestOnBorrow(false); 
		jedisPool = new JedisPool(config,"127.0.0.1",6379);
	}

	public static Jedis getResource() {
		return jedisPool.getResource();
	}

	public static void returnResource(Jedis jedis) {
		jedisPool.returnResource(jedis);
	}

}
