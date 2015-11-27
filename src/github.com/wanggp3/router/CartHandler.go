package router

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
	"strconv"

	"github.com/garyburd/redigo/redis"
	"github.com/gorilla/mux"
	"github.com/wanggp3/model"
	"github.com/wanggp3/util"
)

const (
	KEY_CART_ID      = "cart_id"
	KEY_CART_USER    = "cart_user:"
	KEY_CART_CONTENT = "cart_content:"
	KEY_CART_TOTAL   = "cart_total:"
)

type CartIdStruct struct {
	Cart_id string `json:"cart_id"`
}

type ReponseResult struct {
	Status  int
	Code    string
	Message string
}

type FoodPara struct {
	Food_id int `json:"food_id"`
	Count   int `json:"count"`
}

func createCart(user model.User) string {
	con := util.RedisPool.Get()
	defer con.Close()

	cartid, _ := redis.Int(con.Do("incr", KEY_CART_ID))
	ret := strconv.Itoa(cartid)
	con.Do("set", KEY_CART_USER+ret, user.Id)

	return ret
}

func tryAddFood(cartId string, food_id, food_count int) bool {
	con := util.RedisPool.Get()
	defer con.Close()
	cur, err := redis.Int(con.Do("incrby", KEY_CART_TOTAL+cartId, food_count))
	if err != nil {
		return false
	}
	if cur <= 3 {
		con.Do("hincrby", KEY_CART_CONTENT+cartId, food_id, food_count)
		return true
	} else {
		con.Do("incrby", KEY_CART_TOTAL+cartId, -food_count)
		return false
	}

	return true
}
func addFood(user model.User, food_id int, food_count int, cartId string) ReponseResult {
	con := util.RedisPool.Get()
	defer con.Close()
	var r ReponseResult
	uid, ok := redis.Int(con.Do("get", KEY_CART_USER+cartId))
	if ok != nil {
		r = ReponseResult{404, "CART_NOT_FOUND", "篮子不存在"}
		return r
	}

	if uid != user.Id {
		r = ReponseResult{401, "NOT_AUTHORIZED_TO_ACCESS_CART", "无权限访问指定的篮子"}
		return r
	}

	if model.FoodExist(food_id) == false {
		r = ReponseResult{404, "FOOD_NOT_FOUND", "食物不存在"}
		return r
	}

	if food_count > 3 || tryAddFood(cartId, food_id, food_count) == false {
		r = ReponseResult{403, "FOOD_OUT_OF_LIMIT", "篮子中食物数量超过了三个"}
		return r
	}

	r = ReponseResult{204, "xx", "xx"}
	return r
}

func CartHandler(w http.ResponseWriter, r *http.Request) {
	//log.Println("in cartHandler, url = ", r.URL)
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Println("read err : ", err)
	}
	if err = r.Body.Close(); err != nil {
		log.Println("close err body err :", err)
	}
	var para FoodPara
	if len(body) != 0 {
		if err = json.Unmarshal(body, &para); err != nil {
			log.Println("unmarshal err ", err)
		}
	}
	cartId := mux.Vars(r)["cartId"]

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

	if r.Method == "POST" {
		//log.Println("should be post method ", r.Method)
		cartId := createCart(user)
		w.WriteHeader(200)
		ans := CartIdStruct{cartId}
		if err := json.NewEncoder(w).Encode(ans); err != nil {
			log.Println(err)
		}
	} else if r.Method == "PATCH" {
		//log.Println("should be path method ", r.Method)
		res2 := addFood(user, para.Food_id, para.Count, cartId)
		if res2.Status == 204 {
			w.WriteHeader(204)
			return
		} else {
			w.WriteHeader(res2.Status)
			res3 := Result{res2.Code, res2.Message}
			if err := json.NewEncoder(w).Encode(res3); err != nil {
				log.Println(err)
			}
			return
		}
	}
}
