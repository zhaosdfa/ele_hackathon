package model

type Food struct {
	Id    int `json:"id"`
	Stock int `json:"stock"`
	Price int `json:"price"`
}
type FoodSlice struct {
	Foods []Food
}

var (
	IdToPrice map[int]int
	FoodList  FoodSlice
)

func FoodExist(food_id int) bool {
	_, ok := IdToPrice[food_id]
	return ok
}
