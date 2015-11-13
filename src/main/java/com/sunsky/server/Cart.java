package com.sunsky.server;

public class Cart {

	public String id;

	public int userId;

	public Food[] foods;

	public int count;

	public Cart() {
		init();
	}

	public Cart(String id, int userId) {
		this.id = id;
		this.userId = userId;
		init();
	}

	private void init() {
		foods = new Food[3];
		count = 0;
	}

}
