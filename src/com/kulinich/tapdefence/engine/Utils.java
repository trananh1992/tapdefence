package com.kulinich.tapdefence.engine;

public class Utils {
	public static int randsgn() {
		int r = (int) Math.round(Math.random());
		if (r == 0) {
			return -1;
		} else {
			return 1;
		}
	}

	public static float transition(float ratio, float a, float b) {
		return a + (b - a) * (1 - ratio);
	}

	public static float metrics(Point a, Point b) {
		float xs = b.x - a.x;
		float ys = b.y - a.y;
		return (float) (Math.sqrt(xs * xs + ys * ys));
	}

	// (c) Alexander Hristov
	// http://www.ahristov.com/tutorial/geometry-games/intersection-segments.html
	public static Point intersect(Line a, Line b) {
		float x1 = a.a.x;
		float x2 = a.b.x;
		float x3 = b.a.x;
		float x4 = b.b.x;
		float y1 = a.a.y;
		float y2 = a.b.y;
		float y3 = b.a.y;
		float y4 = b.b.y;
		float d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
		if (d == 0) {
			return null;
		}
		float xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2)
				* (x3 * y4 - y3 * x4))
				/ d;
		float yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2)
				* (x3 * y4 - y3 * x4))
				/ d;

		Point p = new Point(xi, yi);
		if (xi < Math.min(x1, x2) || xi > Math.max(x1, x2)) {
			return null;
		}
		if (xi < Math.min(x3, x4) || xi > Math.max(x3, x4)) {
			return null;
		}
		return p;
	}
}
