package com.sunsky.server;

import redis.clients.jedis.*;
import java.util.*;
import org.json.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class CartDAO {

    private static final String KEY_CART_ID = "cart_id";

    private static final String KEY_CART_CONTENT = "cart_content:";

    private static final String KEY_CART_USR = "cart_user:";

    private static final String KEY_CART_TOTAL = "cart_total:";

    private static final String KEY_ORDER_CONTENT = "order_content:";

    private static final String KEY_ALL_ORDERS = "all_orders";

    private static int failCount = 0;

    private static int REDIS_TIME_OUT_LIMIT = 2;

    /**
     * return cart id
     */
    public static String createCart(int userId) {
        Jedis jedis = RedisClient.getResource();

        String cartId = null;

        for (int i = 0; i < REDIS_TIME_OUT_LIMIT; i++) {
            try {
                cartId = jedis.incr(KEY_CART_ID).toString();
                break ;
            } catch (JedisConnectionException e) {
                Utils.print(e);
                //RedisClient.returnResource(jedis);
                //jedis = RedisClient.getResource();
            }
        }


        Pipeline pipe = jedis.pipelined();

        pipe.set(KEY_CART_USR + cartId, ""+userId);

        // faster than jedis.set
        pipe.sync();

        RedisClient.returnResource(jedis);
        return cartId;
    }

    //private static Map<String, String> CartHolder = new HashMap<String, String>();

    /**
     * return null if not exists.
     */
    public static String getUserId(String cartId) {
        //String userId = CartHolder.get(cartId);
        //if (userId != null) {
        //    return userId;
        //}
        Jedis jedis = RedisClient.getResource();
        String userId = null;
        for (int i = 0; i < REDIS_TIME_OUT_LIMIT; i++) {
            try {
                userId = jedis.get(KEY_CART_USR + cartId);
                break ;
            } catch (Exception e) {
                Utils.print(e);
                //RedisClient.returnResource(jedis);
                //jedis = RedisClient.getResource();
            }
        }
        RedisClient.returnResource(jedis);
        //CartHolder.put(cartId, userId);
        return userId;
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
        boolean flag = true;
        try {
            int cur = jedis.incrBy(KEY_CART_TOTAL + cartId, foodCount).intValue();
            // should judge cnt < 0 ?
            if (cur > 3) {
                // roll back
                jedis.incrBy(KEY_CART_TOTAL + cartId, -foodCount);
                flag = false;
            }
            if (flag) {
                jedis.hincrBy(KEY_CART_CONTENT + cartId, ""+foodId, foodCount);
            }
        } catch (Exception e) {
            Utils.print(e);
        } finally {
            RedisClient.returnResource(jedis);
        }
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

        //	String orderId = jedis.hset(KEY_USR_ORDER + userId);

        RedisClient.returnResource(jedis);
        return  null;
    }


    public enum Result {OK, OUT_OF_LIMIT, OUT_OF_STOCK, FAIL};

    public static Result tryOrder(String cartId, int userId) {
        //return _tryOrderInterface(cartId, userId);
        return _tryOrderV2Interface(cartId, userId);
        //return _tryOrderV3Interface(cartId, userId);
    }

    private static final int TRY_ORDER_LIMIT = 3; // times of try order

    private static Result _tryOrderV3Interface(String cartId, int userId) {
        Jedis jedis = RedisClient.getResource();
        Result res = null;
        for (int i = 0; i < 2; i++) {
            try {
                res = _tryOrderV3(cartId, userId, jedis);
                break;
            } catch (Exception e) {
                RedisClient.returnResource(jedis);
                jedis = RedisClient.getResource();
            }
        }
        RedisClient.returnResource(jedis);
        return res;
    }

    private static Result _tryOrderV2Interface(String cartId, int userId) {
        Jedis jedis = RedisClient.getResource();
        Result res = null;
        for (int i = 0; i < 2; i++) {
            try {
                res = _tryOrderV2(cartId, userId, jedis);
                break;
            } catch (Exception e) {
                RedisClient.returnResource(jedis);
                jedis = RedisClient.getResource();
            }
        }
        RedisClient.returnResource(jedis);
        return res;
    }

    public static Result _tryOrderInterface(String cartId, int userId) {
        Jedis jedis = RedisClient.getResource();
        Result res = null;
        for (int i = 0; i < TRY_ORDER_LIMIT; i++) {
            try {
                res = _tryOrder(cartId, userId, jedis);
                if (res != Result.FAIL) break;
            } catch (JedisConnectionException e) {
                RedisClient.returnResource(jedis);
                jedis = RedisClient.getResource();
            }
        }
        if (res == Result.FAIL) {
            try {
                justDoIt(cartId, userId, jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
            res = Result.OK;
        }
        RedisClient.returnResource(jedis);
        return res;
    }

    private static void justDoIt(String cartId, int userId, Jedis jedis) {
        Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);

        JSONObject ord = new JSONObject();
        ord.put("id", cartId);
        ord.put("user_id", userId);

        int total = 0;

        Pipeline pipe = jedis.pipelined();

        JSONArray items = new JSONArray();
        int itemsIndex = 0;

        pipe.set(KEY_USR_ORDER_ID + userId, cartId);
        for (Map.Entry<String, String> entry : foods.entrySet()) {
            int fid = -1;
            int cnt = -1;
            try {
                fid = Integer.parseInt(entry.getKey());
                cnt = Integer.parseInt(entry.getValue());
            } catch (Exception e) {
                Utils.print(e);
            }
            total += FoodsDAO.getPrice(fid) * cnt;
            JSONObject it = new JSONObject();
            it.put("food_id", fid);
            it.put("count", cnt);
            items.put(itemsIndex++, it);
            if (cnt > 0) {
                pipe.incrBy(FoodsDAO.KEY_FOOD_STOCK + fid, -cnt);
            }
        }
        ord.put("total", total);
        ord.put("items", items);

        pipe.hmset(KEY_ORDER_CONTENT + cartId, foods);
        pipe.lpush(KEY_ALL_ORDERS, ord.toString());
        pipe.sync();

        return ;
    }

    // test
    private static final String KEY_USR_ORDER_NUM = "user_order_num:";
    private static Result _tryOrderV2(String cartId, int userId, Jedis jedis) {
        int orderNum = jedis.incrBy(KEY_USR_ORDER_NUM + userId, 1).intValue();
        if (orderNum > 1) {
            // it is not a necessity
            // jedis.incrBy(KEY_USR_ORDER_NUM + userId, -1);
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
        Iterator<Map.Entry<String, String>> foodsIt = foods.entrySet().iterator();
        while (foodsIt.hasNext()) {
            Map.Entry<String, String> entry = foodsIt.next();
            int fid = -1;
            int cnt = -1;
            try {
                fid = Integer.parseInt(entry.getKey());
                cnt = Integer.parseInt(entry.getValue());
            } catch (NumberFormatException e) {
                continue ;
            }
            if (cnt > 0) {
                int t = jedis.incrBy(FoodsDAO.KEY_FOOD_STOCK + entry.getKey(), -cnt).intValue();
                if (t < 0) {
                    outOfStock = true;
                    continue ;
                }
                JSONObject it = new JSONObject();
                it.put("food_id", fid);
                it.put("count", cnt);
                items.put(itemsIndex++, it);

                total += FoodsDAO.getPrice(fid) * cnt;
            } else {
                foodsIt.remove();
            }
        }

        if (outOfStock) {
            jedis.incrBy(KEY_USR_ORDER_NUM + userId, -1);
            for (Map.Entry<String, String> entry : foods.entrySet()) {
                int fid = -1;
                int cnt = -1;
                try {
                    fid = Integer.parseInt(entry.getKey());
                    cnt = Integer.parseInt(entry.getValue());
                } catch (NumberFormatException e) {
                    continue ;
                }
                if (cnt > 0) {
                    jedis.incrBy(FoodsDAO.KEY_FOOD_STOCK + entry.getKey(), cnt).intValue();
                }
            }
            return Result.OUT_OF_STOCK;
        }

        jedis.hmset(KEY_ORDER_CONTENT + cartId, foods);

        jedis.set(KEY_USR_ORDER_ID + userId, cartId);

        ord.put("total", total);
        ord.put("items", items);

        // MARK 
        jedis.lpush(KEY_ALL_ORDERS, ord.toString());

        return Result.OK;
    }

    //test v3
    private static Result _tryOrderV3(String cartId, int userId, Jedis jedis) {
        String orderId = jedis.get(KEY_USR_ORDER_ID + userId);
        if (orderId != null) {
            return Result.OUT_OF_LIMIT;
        }

        Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);
        JSONObject ord = new JSONObject();
        ord.put("id", cartId);
        ord.put("user_id", userId);

        int total = 0;
        JSONArray items = new JSONArray();
        int itemsIndex = 0;
        List<String> KEYS = new ArrayList<String>();
        List<String> ARGV = new ArrayList<String>();
        /*
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
           */
        Iterator<Map.Entry<String, String>> foodsIt = foods.entrySet().iterator();
        while (foodsIt.hasNext()) {
            Map.Entry<String, String> entry = foodsIt.next();
            try {
                int fid = Integer.parseInt(entry.getKey());
                int cnt = Integer.parseInt(entry.getValue());
                if (cnt > 0) {
                    KEYS.add(entry.getKey());
                    ARGV.add(entry.getValue());
                    JSONObject it = new JSONObject();
                    it.put("food_id", fid);
                    it.put("count", cnt);
                    items.put(itemsIndex++, it);
                    total += FoodsDAO.getPrice(fid) * cnt;
                }else foodsIt.remove(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ord.put("total", total);
        ord.put("items", items);

        if(KEYS.size() == 1){
            Long re = (Long)jedis.evalsha(LuaScript.sha1,KEYS,ARGV);
            if(re == 1)return Result.OUT_OF_STOCK;
            else if(re == 2)return Result.FAIL;		
            else if(re == 3){
                jedis.hmset(KEY_ORDER_CONTENT + cartId, foods);
                jedis.set(KEY_USR_ORDER_ID + userId, cartId);
                jedis.lpush(KEY_ALL_ORDERS, ord.toString());
                return Result.OK;
            }else{
                Utils.println("redis lua re err with " + re);
            }
        }else if(KEYS.size() == 2){
            Long re = (Long)jedis.evalsha(LuaScript.sha2,KEYS,ARGV);
            if(re == 1)return Result.OUT_OF_STOCK;
            else if(re == 2)return Result.FAIL;		
            else if(re == 3){
                jedis.hmset(KEY_ORDER_CONTENT + cartId, foods);
                jedis.set(KEY_USR_ORDER_ID + userId, cartId);
                jedis.lpush(KEY_ALL_ORDERS, ord.toString());
                return Result.OK;
            }else{
                Utils.println("redis lua re err with " + re);
            }
        }else if(KEYS.size() == 3){
            Long re = (Long)jedis.evalsha(LuaScript.sha3,KEYS,ARGV);
            if(re == 1)return Result.OUT_OF_STOCK;
            else if(re == 2)return Result.FAIL;		
            else if(re == 3){
                jedis.hmset(KEY_ORDER_CONTENT + cartId, foods);
                jedis.set(KEY_USR_ORDER_ID + userId, cartId);
                jedis.lpush(KEY_ALL_ORDERS, ord.toString());
                return Result.OK;
            }else{
                Utils.println("redis lua re err with " + re);
            }

        }else{
            Utils.println("error : KEYS.size() > 3!");
        }
        return Result.OK;
    }

    // TODO 
    private static Result _tryOrder(String cartId, int userId, Jedis jedis) {
        // it is not a necessity
        // jedis.watch(KEY_USR_ORDER_ID + userId, KEY_CART_CONTENT + cartId);

        Map<String, String> foods = jedis.hgetAll(KEY_CART_CONTENT + cartId);

        JSONObject ord = new JSONObject();
        ord.put("id", cartId);
        ord.put("user_id", userId);

        int total = 0;

        JSONArray items = new JSONArray();
        int itemsIndex = 0;

        // watch foods
        Map<String, String> newFoods = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : foods.entrySet()) {
            int fid = -1;
            int cnt = -1;
            try {
                fid = Integer.parseInt(entry.getKey());
                cnt = Integer.parseInt(entry.getValue());
            } catch (NumberFormatException e) {
                continue ;
            }
            if (cnt > 0) {
                jedis.watch(FoodsDAO.KEY_FOOD_STOCK + fid);
                JSONObject it = new JSONObject();
                it.put("food_id", fid);
                it.put("count", cnt);
                items.put(itemsIndex++, it);
                total += FoodsDAO.getPrice(fid) * cnt;
                Long newStock = Long.valueOf(jedis.get(FoodsDAO.KEY_FOOD_STOCK + entry.getKey()))
                    - Long.valueOf(entry.getValue());
                if (newStock.intValue() < 0) {
                    jedis.unwatch();
                    return Result.OUT_OF_STOCK;
                }
                newFoods.put(entry.getKey(), newStock.toString());
            }
        }

        ord.put("total", total);
        ord.put("items", items);

        String orderId = jedis.get(KEY_USR_ORDER_ID + userId);

        if (orderId != null) {
            jedis.unwatch();
            return Result.OUT_OF_LIMIT;
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
            //System.out.println("transaction: -> FAILED");
            Utils.println("transaction: -> FAILED");
            return Result.FAIL;
        }
        for (Object str : list) {
            Utils.println("transaction: -> " + str);
        }
        return Result.OK;
    }

    public static String getOrders() {
        Jedis jedis = RedisClient.getResource();
        List<String> list = jedis.lrange(KEY_ALL_ORDERS, 0, -1);
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
