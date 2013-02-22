package com.kulinich.tapdefence.engine;

import android.util.Log;

class Enemy extends Line {
	boolean visible;

	public Enemy() {
		super();
		this.visible = true;
	}

	public Enemy(Point a, Point b) {
		super(a, b);
		this.visible = true;
	}
}
