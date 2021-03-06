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
	KEY_FOOD_STOCK_INIT = "food_stock_init"
	KEY_FOOD_STOCK      = "food_stock:"
)

func loadUser(db *sql.DB) {
	NameToUser = make(map[string]User)
	AccessTokenToUser = make(map[string]User)

	rows, err := db.Query("select * from user")
	if err != nil {
		log.Println(err)
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
		log.Println(err)
	}
}

func loadFood(db *sql.DB) {
	IdToPrice = make(map[int]int)

	rows, err := db.Query("select * from food")
	if err != nil {
		log.Println(err)
	}
	defer rows.Close()

	con := util.RedisPool.Get()
	defer con.Close()

	init, _ := redis.Int(con.Do("incr", KEY_FOOD_STOCK_INIT))
	var id int
	var stock int
	var price int
	for rows.Next() {
		err := rows.Scan(&id, &stock, &price)
		if init == 1 {
			con.Send("set", KEY_FOOD_STOCK+strconv.Itoa(id), stock)
		}
		if err != nil {
			log.Println(err)
		}
		FoodList.Foods = append(FoodList.Foods, Food{id, stock, price})
		IdToPrice[id] = price
	}
	if err = rows.Err(); err != nil {
		log.Println(err)
	}

	if init == 1 {
		err = con.Flush()
		if err != nil {
			log.Println("init stock err : ", err)
		}
	}

}

var (
    DB *sql.DB
)

func InitMySQL() {
	db_name := os.Getenv("DB_NAME")
	db_host := os.Getenv("DB_HOST")
	db_port := os.Getenv("DB_PORT")
	db_user := os.Getenv("DB_USER")
	db_pass := os.Getenv("DB_PASS")

	mesg := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s", db_user, db_pass, db_host, db_port, db_name)
	//open driven name,and message
    var err error
	DB, err = sql.Open("mysql", mesg)
	if err != nil {
		log.Fatalf("open database error : %s\n", err)
		return
	}
	defer DB.Close()

//    DB.SetMaxOpenConns(266);
//    DB.SetMaxIdleConns(150);

	//test ping-pong
	err = DB.Ping()
	if err != nil {
		log.Fatal("ping database error : %s\n", err)
		return
	}

    /*
    _, err = DB.Exec("DROP TABLE IF EXISTS cart_owner");
    if err != nil {
        log.Fatal(err)
    }
    _, err = DB.Exec("CREATE TABLE IF NOT EXISTS cart_owner (id INT PRIMARY KEY AUTO_INCREMENT, user_id INT)");
    if err != nil {
        log.Fatal(err)
    }
    */

	loadUser(DB)

	loadFood(DB)
}
