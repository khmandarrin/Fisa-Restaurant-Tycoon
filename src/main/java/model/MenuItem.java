package model;

public enum MenuItem {
//	COFFEE("커피", 2000), 
//	SALAD("샐러드", 4000), 
//	PIZZA("피자", 7000), 
//	PASTA("파스타", 9000), 
//	GNOCCHI("뇨끼", 10000); 
	
	COFFEE("커피", 1000), 
	SALAD("샐러드", 1000), 
	PIZZA("피자", 1000), 
	PASTA("파스타", 1000), 
	GNOCCHI("뇨끼", 1000); 

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