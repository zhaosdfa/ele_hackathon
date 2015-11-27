package model

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"strconv"

	"github.com/garyburd/redigo/redis"
	_ "github.com/go-sql-driver/mysql"
	"github.com/wanggp3/util"
)

const (
	alpha               = string("abcdefghijklmnopqrstuvwxyz")
	KEY_FOOD_STOCK_init = "food_stock_init"
	KEY_FOOD_STOCK      = "food_stock:"
)

func loadUser(db *sql.DB) {
	NameToUser = make(map[string]User)
	AccessTokenToUser = make(map[string]User)

	rows, err := db.Query("select * from user")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var id int
	var name string
	var password string
	var accessToken string
	length := len(alpha)
	for rows.Next() {
		err := rows.Scan(&id, &name, &password)
		if err != nil {
			log.Fatal(err)
		}
		accessToken = strconv.Itoa(id) + string(alpha[id%length])
		user := User{id, name, password, accessToken}
		NameToUser[name] = user
		AccessTokenToUser[accessToken] = user
	}
	if err = rows.Err(); err != nil {
		log.Fatal(err)
	}
}

func loadFood(db *sql.DB) {
	IdToPrice = make(map[int]int)

	rows, err := db.Query("select * from food")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	con := util.RedisPool.Get()
	defer con.Close()

	init, _ := redis.Int(con.Do("incr", KEY_FOOD_STOCK_init))
	var id int
	var stock int
	var price int
	for rows.Next() {
		err := rows.Scan(&id, &stock, &price)
		if init == 1 {
			con.Send("set", KEY_FOOD_STOCK+strconv.Itoa(id), stock)
		}
		if err != nil {
			log.Fatal(err)
		}
		FoodList.Foods = append(FoodList.Foods, Food{id, stock, price})
		IdToPrice[id] = price
	}
	if err = rows.Err(); err != nil {
		log.Fatal(err)
	}

	if init == 1 {
		err = con.Flush()
		if err != nil {
			log.Fatal("init stock err : ", err)
		}
	}

}
func init() {

	db_name := os.Getenv("DB_NAME")
	db_host := os.Getenv("DB_HOST")
	db_port := os.Getenv("DB_PORT")
	db_user := os.Getenv("DB_USER")
	db_pass := os.Getenv("DB_PASS")

	mesg := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s", db_user, db_pass, db_host, db_port, db_name)
	//open driven name,and message
	//db, err := sql.Open("mysql", db_user+":"+db_pass+"@tcp("+db_host+":"+db_port+")/"+db_name)
	db, err := sql.Open("mysql", mesg)
	if err != nil {
		log.Fatalf("open database error : %s\n", err)
		return
	}
	defer db.Close()

	//test ping-pong
	err = db.Ping()
	if err != nil {
		log.Fatal("ping database error : %s\n", err)
		return
	}

	loadUser(db)

	loadFood(db)
}
