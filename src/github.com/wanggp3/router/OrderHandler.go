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
	KEY_ORDER_CONTENT = "cart_content:"
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
		log.Fatal(err)
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
	log.Println("key size = ", len(data))
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
			log.Fatal(err)
		}
		if ans == 1 {
			return ans
		}
	} else if len(data) == 2 {
		var getScript = redis.NewScript(2, Script2)
		ans, err := redis.Int(getScript.Do(con, keys[0], keys[1], args[0], args[1]))
		if err != nil {
			log.Fatal(err)
		}
		if ans == 1 {
			return ans
		}
	} else if len(data) == 3 {
		var getScript = redis.NewScript(3, Script3)
		ans, err := redis.Int(getScript.Do(con, keys[0], keys[1], keys[2], args[0], args[1], args[2]))
		if err != nil {
			log.Fatal(err)
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
	//log.Printf("%T %v\n", o, o)
	//log.Fatal("haha")
	con.Do("lpush", KEY_ALL_ORDERS, o)
	return 3
}

func OrderHandler(w http.ResponseWriter, r *http.Request) {
	log.Println("in orderhandler, url =", r.URL)
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Fatal("read err", err)
	}
	if err = r.Body.Close(); err != nil {
		log.Fatal("close err : ", err)
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
			log.Fatal(err)
		}
		return
	}

	//w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	if r.Method == "GET" {
		log.Println("in orderhandler method = get")
		res := getOrderDetail(user)
		w.WriteHeader(200)
		var ans = make([]OrderDetail, 0)
		if res.Id != "N" {
			ans = append(ans, res)
		}
		if err := json.NewEncoder(w).Encode(ans); err != nil {
			log.Fatal(err)
		}
		log.Println("order 's detail = ", res)
		return
	} else if r.Method == "POST" {
		log.Println("in orderhandler method = post")
		var res Result
		if len(body) == 0 {
			w.WriteHeader(400)
			res = Result{"EMPTY_REQUEST", "请求体为空"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}
		var para CartIdStruct
		if err = json.Unmarshal(body, &para); err != nil || para.Cart_id == "" {
			w.WriteHeader(400)
			res = Result{"MALFORMED_JSON", "格式错误"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}

		con := util.RedisPool.Get()
		defer con.Close()

		var cartId = para.Cart_id
		uid, ok := redis.Int(con.Do("get", KEY_CART_USER+cartId))
		if ok != nil {
			w.WriteHeader(404)
			res = Result{"CART_NOT_FOUND", "篮子不存在"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}

		if uid != user.Id {
			log.Println("not_author_to_access_cart")
			w.WriteHeader(401)
			res = Result{"NOT_AUTHORIZED_TO_ACCESS_CART", "无权限访问指定的篮子"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}

		oid, _ := redis.String(con.Do("get", KEY_USER_ORDER+strconv.Itoa(user.Id)))
		//log.Println("hhahha === == = = = > ", (err != nil), "|", (oid != ""))
		if oid != "" {
			log.Println("out_of_limit")
			w.WriteHeader(403)
			res = Result{"ORDER_OUT_OF_LIMIT", "每个用户只能下一单"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}

		ans := tryOrder(cartId, user.Id)
		log.Println("tryOrder return ", ans)
		if ans == 3 {
			w.WriteHeader(200)
			order := OrderIdStruct{cartId}
			if err = json.NewEncoder(w).Encode(order); err != nil {
				log.Fatal(err)
			}
			return
		} else if ans == 1 {
			log.Println("out_of_stock")
			w.WriteHeader(403)
			res = Result{"FOOD_OUT_OF_STOCK", "食物库存不足"}
			if err = json.NewEncoder(w).Encode(res); err != nil {
				log.Fatal(err)
			}
			return
		}
	}

}
