package util

import (
    "strconv"
	"log"
	"os"
	"time"

	"github.com/garyburd/redigo/redis"
)

func newPool() *redis.Pool {
	redis_host := os.Getenv("REDIS_HOST")
	redis_port := os.Getenv("REDIS_PORT")
	return &redis.Pool{
		MaxIdle: 200,
		//MaxActive:   400,
		IdleTimeout: 40 * time.Second,
		Dial: func() (redis.Conn, error) {
			c, err := redis.Dial("tcp", redis_host+":"+redis_port)
			if err != nil {
				log.Println(err)
				return nil, err
			}
			return c, err
		},
	}
}

var (
	RedisPool *redis.Pool
)

func init() {
	RedisPool = newPool()
	conn := RedisPool.Get()
	defer conn.Close()
	//in my own machine, should be remove in three server
	conn.Do("flushall")
    maxcon, err := redis.Strings(conn.Do("CONFIG", "GET", "maxclients"))
    if err != nil {
        log.Println(err)
    }
    maxc, err := strconv.Atoi(maxcon[1])
    if err != nil {
        maxc = 1200
        log.Println(err)
    }
    if maxc < 1200 {
        maxc = 1200
    }
    log.Println(maxc)
    _, err = conn.Do("CONFIG", "SET", "maxclients", strconv.Itoa(maxc))
    if err != nil {
        log.Println(err)
    }
}
