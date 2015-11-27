package router

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/wanggp3/model"
)

func FoodHandler(w http.ResponseWriter, r *http.Request) {
	//log.Println("in foodhandler,url = ", r.URL)
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
		if _, ok := model.AccessTokenToUser[accessToken]; ok == true {
			w.WriteHeader(200)
			if err := json.NewEncoder(w).Encode(model.FoodList.Foods); err != nil {
				log.Println(err)
			}
			return
		}
	}

	w.WriteHeader(401)
	res = Result{"INVALID_ACCESS_TOKEN", "无效的令牌"}
	if err := json.NewEncoder(w).Encode(res); err != nil {
		log.Println(err)
	}
}
