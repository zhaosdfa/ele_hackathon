package model

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
