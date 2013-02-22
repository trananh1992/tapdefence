package com.kulinich.tapdefence.engine;


class Point {
	float x;
	float y;

	public Point() {
	};

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Point(Point point) {
		x = point.x;
		y = point.y;
	}
}
