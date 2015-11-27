package router

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"

	"github.com/wanggp3/model"
)

type Parameter struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type Result struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

type ResultOk struct {
	User_id     int    `json:"user_id"`
	Username    string `json:"username"`
	AccessToken string `json:"access_token"`
}

func LoginHandler(w http.ResponseWriter, r *http.Request) {
	log.Println("in loginhander, url = ", r.URL)
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Fatal("read err", err)
	}

	if err = r.Body.Close(); err != nil {
		log.Fatal("close err : ", err)
	}
	//w.Header().Set("Content-Type", "application/json;charset=UTF-8")
	var res Result
	if len(body) == 0 {
		w.WriteHeader(400)
		res = Result{"EMPTY_REQUEST", "请求体为空"}
		if err = json.NewEncoder(w).Encode(res); err != nil {
			log.Fatal(err)
		}
		return
	}

	var para Parameter
	if err = json.Unmarshal(body, &para); err != nil || para.Username == "" || para.Password == "" {
		w.WriteHeader(400)
		res = Result{"MALFORMED_JSON", "格式错误"}
		if err = json.NewEncoder(w).Encode(res); err != nil {
			log.Fatal(err)
		}
		return
	}

	user, ok := model.NameToUser[para.Username]
	if ok == false || user.Password != para.Password {
		log.Println("loginhandler password error")
		w.WriteHeader(403)
		res = Result{"USER_AUTH_FAIL", "用户名或密码错误"}
		if err = json.NewEncoder(w).Encode(res); err != nil {
			log.Fatal(err)
		}
		return
	}

	ans := ResultOk{user.Id, user.Name, user.AccessToken}
	w.WriteHeader(200)
	if err = json.NewEncoder(w).Encode(ans); err != nil {
		log.Fatal(err)
	}
}
