package router

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/garyburd/redigo/redis"
	"github.com/wanggp3/model"
	"github.com/wanggp3/util"
)

const (
	KEY_ALL_ORDERS = "all_orders"
)

type AdminOderStruct struct {
	Id      string     `json:"id"`
	User_id int        `json:"user_id"`
	Items   []FoodPara `json:"items"`
	Total   int        `json:"total"`
}

func AdminHandler(w http.ResponseWriter, r *http.Request) {
	log.Println("in Adminhandler,url = ", r.URL)
	r.ParseForm()
	var accessToken string
	if len(r.Form["access_token"]) > 0 {
		accessToken = r.Form["access_token"][0]
	}
	if accessToken == "" {
		accessToken = r.Header.Get("Access-Token")
	}
	//w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	var res Result
	if accessToken != "" {
		if user, ok := model.AccessTokenToUser[accessToken]; ok == true && user.Name == "root" {
			w.WriteHeader(200)
			con := util.RedisPool.Get()
			defer con.Close()
			data, err := redis.ByteSlices(con.Do("lrange", KEY_ALL_ORDERS, 0, -1))

			if err != nil {
				log.Println("fuck in admin ", err)
			}
			//log.Printf("==================================%v %v\n", data, data)

			var obj []AdminOderStruct
			for _, d := range data {
				var x AdminOderStruct
				err = json.Unmarshal(d, &x)
				if err != nil {
					log.Fatal("!! ", err)
				}
				obj = append(obj, x)
			}

			if err := json.NewEncoder(w).Encode(obj); err != nil {
				log.Fatal(err)
			}
			/*var ans = make([]AdminOderStruct)
			for _, v := range data {
				ans = append(ans, v)
			}
			if err := json.NewEncoder(w).Encode(ans); err != nil {
				log.Fatal(err)
			}*/
			return
		}
	}

	w.WriteHeader(401)
	res = Result{"INVALID_ACCESS_TOKEN", "无效的令牌"}
	if err := json.NewEncoder(w).Encode(res); err != nil {
		log.Fatal(err)
	}
}
