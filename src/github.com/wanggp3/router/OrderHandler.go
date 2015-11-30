package router

import (
	"io/ioutil"
	"log"
	"net/http"
	"strconv"

	"encoding/json"
	"github.com/garyburd/redigo/redis"
	"github.com/wanggp3/model"
	"github.com/wanggp3/util"
)

const (
	KEY_USER_ORDER    = "user_order:"
	KEY_USER_ORDER_CNT    = "user_order:"
	KEY_ORDER_CONTENT = "cart_content:"
	KEY_FOOD_STOCK      = "food_stock:"
)

var (
	Script1 = "local ok = 1; local key_food_stock = 'food_stock:'; if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end; if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); return 3; else return 1; end;"
	Script2 = "local ok = 1; local key_food_stock = 'food_stock:'; if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end; if tonumber(redis.call('GET',key_food_stock..KEYS[2])) < tonumber(ARGV[2]) then ok = 0; end; if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); redis.call('INCRBY',key_food_stock..KEYS[2],-tonumber(ARGV[2])); return 3; else return 1; end;"
	Script3 = "local ok = 1; local key_food_stock = 'food_stock:'; if tonumber(redis.call('GET',key_food_stock..KEYS[1])) < tonumber(ARGV[1]) then ok = 0; end; if tonumber(redis.call('GET',key_food_stock..KEYS[2])) < tonumber(ARGV[2]) then ok = 0; end; if tonumber(redis.call('GET',key_food_stock..KEYS[3])) < tonumber(ARGV[3]) then ok = 0; end; if ok == 1 then redis.call('INCRBY',key_food_stock..KEYS[1],-tonumber(ARGV[1])); redis.call('INCRBY',key_food_stock..KEYS[2],-tonumber(ARGV[2])); redis.call('INCRBY',key_food_stock..KEYS[3],-tonumber(ARGV[3])); return 3; else return 1; end;"
)

type FoodInOrder struct {
	Food_id int `json:"food_id"`
	Count   int `json:"count"`
}

type FoodInOrderSlice struct {
	FoodList []FoodInOrder
}

type OrderDetail struct {
	Id    string        `json:"id"`
	Items []FoodInOrder `json:"items"`
	Total int           `json:"total"`
}

type OrderDetailSlice struct {
	OrderContent []OrderDetail
}

type OrderIdStruct struct {
	Id string `json:"id"`
}

func getOrderDetail(user model.User) OrderDetail {
	con := util.RedisPool.Get()
	defer con.Close()

	orderId, ok := redis.String(con.Do("get", KEY_USER_ORDER+strconv.Itoa(user.Id)))
	if ok != nil {
		return OrderDetail{Id: "N"}
	}

	data, err := redis.IntMap(con.Do("hgetall", KEY_ORDER_CONTENT+orderId))
	if err != nil {
		log.Println(err)
		return OrderDetail{Id: "N"}
	}

	var ans OrderDetail
	var foodList []FoodInOrder
	var total = 0
	for k, v := range data {
		kk, _ := strconv.Atoi(k)
		price := model.IdToPrice[kk]
		total += price * v
		foodList = append(foodList, FoodInOrder{kk, v})
	}

	ans.Id = orderId
	ans.Items = foodList
	ans.Total = total
	return ans
}

func tryOrder(cartId string, userId int) int {
	con := util.RedisPool.Get()
	defer con.Close()
	data, _ := redis.IntMap(con.Do("hgetall", KEY_CART_CONTENT+cartId))
	//log.Println("key size = ", len(data))
	keys := make([]string, 0, len(data))
	args := make([]int, 0, len(data))
	var order AdminOderStruct
	order.Id = cartId
	order.User_id = userId
	var total = 0
	var items []FoodPara
	for k, v := range data {
		keys = append(keys, k)
		args = append(args, v)
		kk, _ := strconv.Atoi(k)
		items = append(items, FoodPara{kk, v})
		total += v * kk
	}
	order.Items = items
	order.Total = total
	if len(data) == 1 {
		var getScript = redis.NewScript(1, Script1)
		ans, err := redis.Int(getScript.Do(con, keys[0], args[0]))
		if err != nil {
			log.Println(err)
		}
		if ans == 1 {
			return ans
		}
	} else if len(data) == 2 {
		var getScript = redis.NewScript(2, Script2)
		ans, err := redis.Int(getScript.Do(con, keys[0], keys[1], args[0], args[1]))
		if err != nil {
			log.Println(err)
		}
		if ans == 1 {
			return ans
		}
	} else if len(data) == 3 {
		var getScript = redis.NewScript(3, Script3)
		ans, err := redis.Int(getScript.Do(con, keys[0], keys[1], keys[2], args[0], args[1], args[2]))
		if err != nil {
			log.Println(err)
		}
		if ans == 1 {
			return ans
		}
	}
	con.Do("set", KEY_USER_ORDER+strconv.Itoa(userId), cartId)
	o, err := json.Marshal(order)
	if err != nil {
		log.Println("encode order err:", err)
	}
	con.Do("lpush", KEY_ALL_ORDERS, o)
	return 3
}

func OrderHandler(w http.ResponseWriter, r *http.Request) {
	//log.Println("in orderhandler, url =", r.URL)
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Println("read err", err)
	}
	if err = r.Body.Close(); err != nil {
		log.Println("close err : ", err)
	}

	r.ParseForm()
	var accessToken string
	if len(r.Form["access_token"]) > 0 {
		accessToken = r.Form["access_token"][0]
	}
	if accessToken == "" {
		accessToken = r.Header.Get("Access-Token")
	}

	var user model.User
	var ok bool
	if user, ok = model.AccessTokenToUser[accessToken]; ok == false || accessToken == "" {
		w.WriteHeader(401)
		res := Result{"INVALID_ACCESS_TOKEN", "无效的令牌"}
		if err := json.NewEncoder(w).Encode(res); err != nil {
			log.Println(err)
		}
		return
	}

	//w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	if r.Method == "GET" {
		res := getOrderDetail(user)
		w.WriteHeader(200)
		var ans = make([]OrderDetail, 0)
		if res.Id != "N" {
			ans = append(ans, res)
		}
		if err := json.NewEncoder(w).Encode(ans); err != nil {
			log.Println(err)
		}
		return
	} else if r.Method == "POST" {
		var res Result
		if len(body) == 0 {
			w.WriteHeader(400)
			res = Result{"EMPTY_REQUEST", "请求体为空"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}
		var para CartIdStruct
		if err = json.Unmarshal(body, &para); err != nil || para.Cart_id == "" {
			w.WriteHeader(400)
			res = Result{"MALFORMED_JSON", "格式错误"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}

		//con := util.RedisPool.Get()
		//defer con.Close()


		var cartId = para.Cart_id

        /*
        rows, err := model.DB.Query("SELECT user_id FROM cart_owner WHERE id=?", cartId)
        if err != nil {
            log.Fatal(err)
        }
        defer rows.Close()

        var uid int
        var ok bool = true
        if rows.Next() {
            err = rows.Scan(&uid)
            if err != nil {
                log.Fatal(err)
            }
        } else {
            ok = false
        }
        */

        resId := tryOrder1(cartId, user.Id)

		if resId == 10 {
			w.WriteHeader(404)
			res = Result{"CART_NOT_FOUND", "篮子不存在"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}

		if resId == 9 {
			w.WriteHeader(401)
			res = Result{"NOT_AUTHORIZED_TO_ACCESS_CART", "无权限访问指定的篮子"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}

		//oid, _ := redis.String(con.Do("get", KEY_USER_ORDER+strconv.Itoa(user.Id)))
		if resId == 8 {
			//log.Println("out_of_limit")
			w.WriteHeader(403)
			res = Result{"ORDER_OUT_OF_LIMIT", "每个用户只能下一单"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}
        if resId == 3 {
			w.WriteHeader(403)
			res = Result{"FOOD_OUT_OF_STOCK", "食物库存不足"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Println(err)
			}
			return
		}
		//ans := tryOrder(cartId, user.Id)
		if resId == 1 {
			w.WriteHeader(200)
			order := OrderIdStruct{cartId}
			if err = json.NewEncoder(w).Encode(order); err != nil {
				log.Println(err)
			}
			return
		}
	}

}
func tryOrder1(cartId string, userId int) int {
    con := util.RedisPool.Get()
    defer con.Close()

    uid, ok := redis.Int(con.Do("get", KEY_CART_USER+cartId))
    if ok != nil {
        return 10
    }

    if uid != userId {
        return 9
    }

    ordcnt, _ := redis.Int(con.Do("incr", KEY_USER_ORDER_CNT + strconv.Itoa(userId)))
    if ordcnt > 1 {
        return 8
    }

    data, _ := redis.IntMap(con.Do("hgetall", KEY_CART_CONTENT+cartId))
	var order AdminOderStruct
	order.Id = cartId
	order.User_id = userId
    var total = 0
    var items []FoodPara
    var fuck bool = false
    //var times int = 0
    for fid, fcnt := range data {
        if fcnt > 0 {
            fids, _ := strconv.Atoi(fid)
            items = append(items, FoodPara{fids, fcnt})
            total += fcnt * model.IdToPrice[fids]
            remain, _ := redis.Int(con.Do("decrby", KEY_FOOD_STOCK + fid, fcnt))
            //times++
            if remain < 0 {
                fuck = true
            //    break;
            }
        }
    }
    if fuck {
        con.Do("decr", KEY_USER_ORDER_CNT + strconv.Itoa(userId))
        //var cnt int = 0
        for fid, fcnt := range data {
            //cnt ++
            con.Do("incrby", KEY_FOOD_STOCK + fid, fcnt)
            //if cnt == times {
            //    break
            //}
        }
        return 3
    }
	con.Do("set", KEY_USER_ORDER+strconv.Itoa(userId), cartId)
	order.Items = items
	order.Total = total
	o, err := json.Marshal(order)
	if err != nil {
		log.Println("encode order err:", err)
	}
	con.Do("lpush", KEY_ALL_ORDERS, o)
    return 1
}


