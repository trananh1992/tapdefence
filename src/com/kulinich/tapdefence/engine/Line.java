package com.kulinich.tapdefence.engine;

class Line {
	Point a;
	Point b;
	
	float dx;
	float dy;
	
	float length;

	Line() {};

	Line(Point a, Point b) {
		this.a = a;
		this.b = b;
		calculateParameters();
	}
	
	void calculateParameters() {
		length = Utils.metrics(a, b);
		dx = (b.x - a.x) / length;
		dy = (b.y - a.y) / length;
	}
}