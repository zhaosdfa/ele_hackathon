package main

import (
	"fmt"
	"github.com/gorilla/mux"
	"github.com/wanggp3/router"
	_ "github.com/wanggp3/util"
	"log"
	"net/http"
	"os"
)

func main() {
	host := os.Getenv("APP_HOST")
	port := os.Getenv("APP_PORT")
	if host == "" {
		host = "localhost"
	}
	if port == "" {
		port = "8080"
	}
	addr := fmt.Sprintf("%s:%s", host, port)

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
