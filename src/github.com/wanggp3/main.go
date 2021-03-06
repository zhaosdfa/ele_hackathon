package main

import (
	"fmt"
	"github.com/gorilla/mux"
    "github.com/wanggp3/router"
    "github.com/wanggp3/model"
	_ "github.com/wanggp3/util"
	"log"
	"net/http"
	"os"
	"runtime"
)

func main() {
	cpu := runtime.NumCPU()
    if cpu < 2 {
        cpu = 2
    }
	runtime.GOMAXPROCS(cpu)
	host := os.Getenv("APP_HOST")
	port := os.Getenv("APP_PORT")
	if host == "" {
		host = "localhost"
	}
	if port == "" {
		port = "8080"
	}
	addr := fmt.Sprintf("%s:%s", host, port)

    model.InitMySQL();


	log.Println("add router")
	r := mux.NewRouter()
	r.HandleFunc("/login", router.LoginHandler)
	r.HandleFunc("/foods", router.FoodHandler)
	r.HandleFunc("/carts", router.CartHandler)
	r.HandleFunc("/carts/{cartId}", router.CartHandler)
	r.HandleFunc("/orders", router.OrderHandler)
	r.HandleFunc("/admin/orders", router.AdminHandler)
	log.Println("add router end, server start")

    err := http.ListenAndServe(addr, r)
	log.Fatal(err)
}
