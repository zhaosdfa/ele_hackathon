package util

import (
	"log"
	"os"
	"time"

	"github.com/garyburd/redigo/redis"
)

func newPool() *redis.Pool {
	redis_host := os.Getenv("REDIS_HOST")
	redis_port := os.Getenv("REDIS_PORT")
	return &redis.Pool{
		MaxIdle:     15,
		IdleTimeout: 40 * time.Second,
		Dial: func() (redis.Conn, error) {
			c, err := redis.Dial("tcp", redis_host+":"+redis_port)
			if err != nil {
				log.Fatal(err)
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
}
