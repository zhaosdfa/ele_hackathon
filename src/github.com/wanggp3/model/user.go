package model

import (
	"fmt"
)

type User struct {
	Id          int
	Name        string
	Password    string
	AccessToken string
}

var (
	NameToUser        map[string]User
	AccessTokenToUser map[string]User
)

func TestShowUser() {
	for a, b := range NameToUser {
		fmt.Println(a, b)
	}
}
