package com.kulinich.tapdefence.engine;

import android.graphics.Paint;

class Particle {
	float lifetime = Constants.PARTICLE_LIFETIME;
	
	Paint paint;
	Point point;
	float radius;
	float lived;
	boolean visible;
	float dx;
	float dy;

	public Particle(Point point, Paint paint, float maxRadius) {
		this.point = new Point(point);
		this.paint = new Paint(paint);
		this.radius = 1 + (float) Math.random() * (maxRadius);
		this.lived = 0;
		this.visible = true;
		this.dx = 0;
		this.dy = 0;
	}
}