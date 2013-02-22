package com.kulinich.tapdefence.engine;

class Wall extends Line {
	boolean visible;

	public Wall() {
		super();
		this.visible = true;
	}

	public Wall(Point a, Point b) {
		super(a, b);
		this.visible = true;
	}
}

