package com.sunsky.server;

public class Food {
    private int id;
    private int price;
    private int stock;

    public Food() {
    }

    public Food(int id, int price, int stock) {
	this.id = id;
	this.price = price;
	this.stock = stock;
    }

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    public int getPrice() {
	return price;
    }

    public void setPrice(int price) {
	this.price = price;
    }

    public int getStock() {
	return stock;
    }

    public void setStock(int stock) {
	this.stock = stock;
    }
}
