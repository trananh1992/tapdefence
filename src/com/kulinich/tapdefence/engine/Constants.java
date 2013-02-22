package com.kulinich.tapdefence.engine;

import android.graphics.Paint;

public class Constants {
	static int STATE_PAUSE = 0;
	static int STATE_READY = 1;
	static int STATE_RUNNING = 2;
	static int STATE_LOSE = 3;

	static int STARS = 42;
	static int STAR_MAX_RADIUS = 2;

	static float ALARM_LEVEL = 0.3f;

	static float RUMBLE_WALL_HIT = 10;
	static float RUMBLE_PLAYER_HIT = 20;
	static float RUMBLE_PLAYER_DESTROYED = 20;
	static int   RUMBLE_MAX = 50;

	static float PARTICLE_LIFETIME = 1.3f;
	static float PARTICLE_SPEED = 5;
	static float PARTICLE_MAX_RADIUS = 1;
	static float PARTICLE_SCATTER = 2;
	static float PARTICLES_PER_PX = 0.1f;
	static float PARTICLES_MAX_MULT = 4;
	static int   PARTICLES_PER_POINT = 5;
	static int   PARTICLES_FOR_BIG_EXPLOSION = 50;
	static float PARTICLE_EXPLOSION_LIFETIME = 2.5f;

	static float POWER_INIT = 250;
	static float POWER_MAX = 250;

	static float POWER_RENEWAL_SPEED = 75; // pixels per second

	static float HEALTH_MAX = 100;
	static float HEALTH_RENEWAL_SPEED = 1;

	static float ENEMY_DAMAGE = 15;
	static float ENEMY_SPEED_INIT = 25; // pixels per second
	static float ENEMY_MAX_SPEED = 250;
	static float ENEMY_SPEED_ACC = 2f; // pixels per second^2
	static float ENEMY_PERIOD_INIT = 2f;
	static float ENEMY_MIN_PERIOD = 0.5f;
	static float ENEMY_PERIOD_ACC = -0.025f;

	static float EXPLOSION_TIME = 3;

	// Looks
	static int UI_BAR_HEIGHT = 5;
	static int PLAYER_CIRCLE_RADIUS = 20;
	static int ENEMY_LENGTH = 30;
	static int PLAYER_HEALTH_GLOW_SIZE = 35;
	
	// Paints
	static int[] GOOD_HEALTH_COLOR = { 120, 180, 0 };
	static int[] BAD_HEALTH_COLOR = { 215, 5, 5 };

	static Paint BG_COLOR_PAINT;
	static Paint ALARM_PAINT;
	static Paint STAR_PAINT;
	static Paint PLAYER_PAINT;
	static Paint UI_POWER_PAINT;
	static Paint UI_POWER_GHOST_PAINT;
	static Paint DRAW_WALL_PAINT;
	static Paint FAKE_WALL_PAINT;
	static Paint WALL_PAINT;
	static Paint ENEMY_PAINT;
	static Paint EXPLOSION_PAINT;
	
	static void initializePaints() {
		BG_COLOR_PAINT = new Paint();
		BG_COLOR_PAINT.setAntiAlias(true);
		BG_COLOR_PAINT.setARGB(0, 0, 0, 0);

		ALARM_PAINT = new Paint();
		ALARM_PAINT.setAntiAlias(true);
		ALARM_PAINT.setARGB(255, 220, 10, 10);

		STAR_PAINT = new Paint();
		STAR_PAINT.setAntiAlias(true);
		STAR_PAINT.setARGB(255, 255, 255, 255);

		UI_POWER_PAINT = new Paint();
		UI_POWER_PAINT.setAntiAlias(true);
		UI_POWER_PAINT.setARGB(255, 255, 255, 255);

		UI_POWER_GHOST_PAINT = new Paint();
		UI_POWER_GHOST_PAINT.setAntiAlias(true);
		UI_POWER_GHOST_PAINT.setARGB(64, 255, 255, 255);

		PLAYER_PAINT = new Paint();
		PLAYER_PAINT.setAntiAlias(true);
		PLAYER_PAINT.setARGB(255, 255, 255, 255);

		DRAW_WALL_PAINT = new Paint();
		DRAW_WALL_PAINT.setAntiAlias(true);
		DRAW_WALL_PAINT.setStrokeWidth(3);
		DRAW_WALL_PAINT.setARGB(255, 255, 255, 255);

		WALL_PAINT = new Paint();
		WALL_PAINT.setAntiAlias(true);
		WALL_PAINT.setStrokeWidth(3);
		WALL_PAINT.setARGB(255, 255, 255, 255);

		FAKE_WALL_PAINT = new Paint();
		FAKE_WALL_PAINT.setAntiAlias(true);
		FAKE_WALL_PAINT.setStrokeWidth(1);
		FAKE_WALL_PAINT.setARGB(128, 255, 255, 255);

		ENEMY_PAINT = new Paint();
		ENEMY_PAINT.setAntiAlias(true);
		ENEMY_PAINT.setStrokeWidth(3);
		ENEMY_PAINT.setARGB(255, 220, 10, 10);

		EXPLOSION_PAINT = new Paint();
		EXPLOSION_PAINT.setAntiAlias(true);
		EXPLOSION_PAINT.setARGB(255, 220, 200, 200);
	}
}
