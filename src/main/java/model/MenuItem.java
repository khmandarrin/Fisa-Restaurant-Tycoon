package model;

public enum MenuItem {
	COFFEE("커피", 3000), // 1초
	SALAD("샐러드", 5000), // 2초
	PIZZA("피자", 8000), // 5초
	PASTA("파스타", 9000), // 4초
	GNOCCHI("뇨끼", 11000); // 7초

	private final String name;
	private final int cookTime; // 밀리초(ms) 단위

	MenuItem(String name, int cookTime) {
		this.name = name;
		this.cookTime = cookTime;
	}

	public String getName() {
		return name;
	}

	public int getCookTime() {
		return cookTime;
	}
}