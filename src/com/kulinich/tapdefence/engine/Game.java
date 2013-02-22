package com.kulinich.tapdefence.engine;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.util.Log;

public class Game {
	
	private GameThread mContext;

	int mCanvasWidth;
	int mCanvasHeight;
	
	int mRumbleAmount;

	boolean mAlarm;
	float mAlarmLevel;

	boolean mExploding;
	float mExplosionLast;

	float mPower;
	float mPowerGhost;
	float mHealth;

	float mEnemyPeriod;
	float mEnemySpeed;

	double mOverallTime;
	long mWallsMade;

	long mLastTime;
	long mLastTimeEnemySpawned;

	int mX;
	int mY;

	// Walls
	volatile List<Wall> mWalls = new ArrayList<Wall>();

	// Enemies
	volatile List<Enemy> mEnemies = new ArrayList<Enemy>();

	// Particles
	volatile List<Particle> mParticles = new ArrayList<Particle>();

	// Stars
	List<Star> mStars = new ArrayList<Star>();
	
	Game(GameThread context) {
		mContext = context;
		
		mCanvasWidth = context.mCanvasWidth;
		mCanvasHeight = context.mCanvasHeight;
		
		mOverallTime = 0;
		mLastTime = System.currentTimeMillis() + 100;
		mLastTimeEnemySpawned = mLastTime;

		// Health
		mHealth = Constants.HEALTH_MAX;

		// Pos
		mX = mCanvasWidth / 2;
		mY = mCanvasHeight / 2;
		mPower = Constants.POWER_INIT;

		// Rumble
		mRumbleAmount = 0;

		// Enemy parameters
		mEnemyPeriod = Constants.ENEMY_PERIOD_INIT;
		mEnemySpeed = Constants.ENEMY_SPEED_INIT;

		mWallsMade = 0;

		makeStars();
	}
	
	void updateAlarm() {
		if (mHealth / Constants.HEALTH_MAX < Constants.ALARM_LEVEL) {
			mAlarm = true;
			mAlarmLevel = 1 - mHealth
					/ (Constants.HEALTH_MAX * Constants.ALARM_LEVEL);
			Constants.ALARM_PAINT.setAlpha(Math.round(mAlarmLevel * 64));
		} else {
			mAlarm = false;
		}
	}

	void updateParticles(double elapsed) {
		for (int i = 0; i < mParticles.size(); ++i) {
			Particle particle = mParticles.get(i);
			particle.lived += elapsed;
			float lived = particle.lived / particle.lifetime;
			if (lived < 1) {
				Paint paint = new Paint(particle.paint);
				paint.setAlpha((int) Math.floor((1 - lived) * 255));
				particle.paint = paint;
				particle.point.x += Constants.PARTICLE_SPEED * particle.dx
						* elapsed;
				particle.point.y += Constants.PARTICLE_SPEED * particle.dy
						* elapsed;
			} else {
				particle.visible = false;
			}
		}
	}

	void updateRumble(double elapsed) {
		mRumbleAmount -= elapsed;
		if (mRumbleAmount <= 0) {
			mRumbleAmount = 0;
		} else if (mRumbleAmount > Constants.RUMBLE_MAX) {
			mRumbleAmount = Constants.RUMBLE_MAX;
		}
	}

	void shake(float amount) {
		mRumbleAmount += amount;
	}

	void updateDifficulty(double elapsed) {
		mEnemyPeriod += Constants.ENEMY_PERIOD_ACC * (float) elapsed;
		if (mEnemyPeriod < Constants.ENEMY_MIN_PERIOD) {
			mEnemyPeriod = Constants.ENEMY_MIN_PERIOD;
		}
		mEnemySpeed += Constants.ENEMY_SPEED_ACC * (float) elapsed;
		if (mEnemySpeed > Constants.ENEMY_MAX_SPEED) {
			mEnemySpeed = Constants.ENEMY_MAX_SPEED;
		}
	}

	void spawnEnemy() {
		int side = (int) Math.floor(Math.random() * 4);
		Point a = new Point();
		switch (side) {
		case 0: {
			a.x = 0;
			a.y = (int) Math.floor(Math.random() * (mCanvasHeight + 1));
			break;
		}
		case 1: {
			a.x = (int) Math.floor(Math.random() * (mCanvasWidth + 1));
			a.y = 0;
			break;
		}
		case 2: {
			a.x = mCanvasWidth;
			a.y = (int) Math.floor(Math.random() * (mCanvasHeight + 1));
			break;
		}
		case 3: {
			a.x = (int) Math.floor(Math.random() * (mCanvasWidth + 1));
			a.y = mCanvasHeight;
			break;
		}
		}

		Point o = new Point(mX, mY);
		float ao = Utils.metrics(a, o);
		Point b = new Point();
		b.x = a.x - Math.round(Constants.ENEMY_LENGTH / ao * (o.x - a.x));
		b.y = a.y - Math.round(Constants.ENEMY_LENGTH / ao * (o.y - a.y));

		Enemy enemy = new Enemy();
		enemy.a = a;
		enemy.b = b;

		mEnemies.add(enemy);
	}

	void updateEnemies(double elapsed) {
		for (int i = 0; i < mEnemies.size(); ++i) {
			Enemy enemy = mEnemies.get(i);

			Point o = new Point(mX, mY);
			Point a = enemy.a;
			Line ao = new Line(a, o);
			float dist = (float) (mEnemySpeed * elapsed);
			float dx = dist * ao.dx;
			float dy = dist * ao.dy;

			enemy.a.x += dx;
			enemy.a.y += dy;
			enemy.b.x += dx;
			enemy.b.y += dy;

			// Check if wall is hit
			// TODO: Fix occasional exceptions
			try {
				for (int j = 0; j < mWalls.size(); ++j) {
					Wall wall = mWalls.get(j);
					Point intersection = Utils.intersect(wall, enemy);
					if (intersection != null) {
						wall.visible = false;
						makeDebris(wall, Constants.WALL_PAINT);
						enemy.visible = false;
						makeDebris(enemy, Constants.ENEMY_PAINT);
						makeDebris(intersection, Constants.WALL_PAINT);
						shake(Constants.RUMBLE_WALL_HIT);
						continue;
					}
				}

				// Check if the player is hit
				float distToPlayer = Utils.metrics(enemy.a, o);
				if (distToPlayer < Constants.PLAYER_CIRCLE_RADIUS) {
					drainHealth(Constants.ENEMY_DAMAGE);
					shake(Constants.RUMBLE_PLAYER_HIT);
					enemy.visible = false;
					makeDebris(enemy, Constants.ENEMY_PAINT);
				}

				// Quick fix
			} catch (IndexOutOfBoundsException e) {
				Log.e("Desync", "OutOfBoundsException caught");
				e.printStackTrace();
			}
		}
	}

	void updateHealth(float points) {
		float restored = mHealth + points;
		if (restored > Constants.HEALTH_MAX) {
			mHealth = Constants.HEALTH_MAX;
		} else {
			mHealth = restored;
		}
	}

	void drainHealth(float damage) {
		updateHealth(-damage);
	}

	void updatePower(float amount) {
		float updated = mPower + amount;
		if (amount < 0) {
			mPowerGhost = mPower;
		}
		if (updated <= 0) {
			mPower = 0;
		} else if (updated > Constants.POWER_MAX) {
			mPower = Constants.POWER_MAX;
		} else {
			mPower = updated;
		}
	}

	void usePower(float amount) {
		updatePower(-amount);
	}

	synchronized void makeWall(Point a, Point b) {
		Wall wall = new Wall();
		wall.a = a;
		wall.b = b;
		mWallsMade++;
		mWalls.add(wall);
	}
	
	private void makeStars() {
		for (int i = 0; i < Constants.STARS; ++i) {
			Star star = new Star();
			star.x = (float) Math.random() * (mCanvasWidth + 1);
			star.y = (float) Math.random() * (mCanvasHeight + 1);
			star.radius = (float) Math.random()
					* (Constants.STAR_MAX_RADIUS + 1);
			star.paint = new Paint(Constants.STAR_PAINT);
			star.paint.setAlpha(1 + (int) Math.round(Math.random() * 128));
			mStars.add(star);
		}
	}

	void makeDebris(Line line, Paint paint) {
		float length = Utils.metrics(line.a, line.b);
		int count = Math.round(length * Constants.PARTICLES_PER_PX);
		float part = length / count;
		float dx = part / length * (line.b.x - line.a.x);
		float dy = part / length * (line.b.y - line.a.y);
		for (int i = 0; i < count; ++i) {
			Point debris = new Point();
			debris.x = line.a.x + dx * i + Utils.randsgn()
					* (float) Math.random() * Constants.PARTICLE_SCATTER;
			debris.y = line.a.y + dy * i + Utils.randsgn()
					* (float) Math.random() * Constants.PARTICLE_SCATTER;
			Particle particle = new Particle(debris, paint,
					Constants.PARTICLE_MAX_RADIUS);
			particle.dx = Utils.randsgn() * (float) Math.random() * 1;
			particle.dy = Utils.randsgn() * (float) Math.random() * 1;
			mParticles.add(particle);
		}
	}

	void makeDebris(Point point, Paint paint) {
		for (int i = 0; i < Constants.PARTICLES_PER_POINT; ++i) {
			Point debris = new Point(point);
			debris.x += Utils.randsgn() * Math.random()
					* Constants.PARTICLE_SCATTER;
			debris.y += Utils.randsgn() * Math.random()
					* Constants.PARTICLE_SCATTER;
			Particle particle = new Particle(debris, paint,
					Constants.PARTICLE_MAX_RADIUS);
			particle.dx = Utils.randsgn() * (float) Math.random()
					* Constants.PARTICLES_MAX_MULT;
			particle.dy = Utils.randsgn() * (float) Math.random()
					* Constants.PARTICLES_MAX_MULT;
			mParticles.add(particle);
		}
	}

	void makePlayerDebris() {
		for (int i = 0; i < Constants.PARTICLES_FOR_BIG_EXPLOSION; ++i) {
			Point debris = new Point();
			debris.x = mX + Utils.randsgn() * (float) Math.random()
					* Constants.PLAYER_CIRCLE_RADIUS;
			debris.y = mY + Utils.randsgn() * (float) Math.random()
					* Constants.PLAYER_CIRCLE_RADIUS;
			Particle particle = new Particle(debris, Constants.PLAYER_PAINT,
					Constants.PARTICLE_MAX_RADIUS + 1);
			particle.lifetime = Constants.PARTICLE_EXPLOSION_LIFETIME;
			particle.dx = Utils.randsgn() * (float) Math.random()
					* Constants.PARTICLES_MAX_MULT;
			particle.dy = Utils.randsgn() * (float) Math.random()
					* Constants.PARTICLES_MAX_MULT;
			mParticles.add(particle);
		}
	}

	void makeExplosion() {
		mExploding = true;
		mAlarm = false;
		mExplosionLast = 0;
		makePlayerDebris();
		
		mContext.mInput.clear();
		mWalls.clear();
		mEnemies.clear();
	}

	void updateExplosion(double elapsed) {
		if (mExploding) {
			float explosionRatio = 1 - mExplosionLast
					/ Constants.EXPLOSION_TIME;
			Constants.EXPLOSION_PAINT
					.setAlpha(Math.round(explosionRatio * 255));
			mExplosionLast += elapsed;
			if (mExplosionLast >= Constants.EXPLOSION_TIME) {
				mContext.endGame();
			}
		}
	}

	Paint makeHealthGradient() {
		float[] good = new float[3];
		Color.RGBToHSV(Constants.GOOD_HEALTH_COLOR[0],
				Constants.GOOD_HEALTH_COLOR[1], Constants.GOOD_HEALTH_COLOR[2],
				good);
		float[] bad = new float[3];
		Color.RGBToHSV(Constants.BAD_HEALTH_COLOR[0],
				Constants.BAD_HEALTH_COLOR[1], Constants.BAD_HEALTH_COLOR[2],
				bad);

		float ratio = mHealth / Constants.HEALTH_MAX;
		float[] blend = new float[3];
		blend[0] = Utils.transition(ratio, good[0], bad[0]);
		blend[1] = Utils.transition(ratio, good[1], bad[1]);
		blend[2] = Utils.transition(ratio, good[2], bad[2]);

		RadialGradient gradient = new RadialGradient(mX, mY,
				Constants.PLAYER_CIRCLE_RADIUS
						+ Constants.PLAYER_HEALTH_GLOW_SIZE,
				Color.HSVToColor(blend), 0x00000000,
				android.graphics.Shader.TileMode.CLAMP);

		Paint result = new Paint();
		result.setDither(true);
		result.setShader(gradient);

		return result;
	}

	long calculateScore() {
		return Math.round(1000 * mOverallTime) - 10 * mWallsMade;
	}
}
