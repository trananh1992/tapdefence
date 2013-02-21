package com.kulinich.tapdefence.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
	
	private int STATE_PAUSE = 0;
	private int STATE_READY = 1;
	private int STATE_RUNNING = 2;
	private int STATE_LOSE = 3;

	private int STARS = 42;
	private int STAR_MAX_RADIUS = 2;

	private float ALARM_LEVEL = 0.2f;

	private float RUMBLE_WALL_HIT = 10;
	private float RUMBLE_PLAYER_HIT = 20;
	private float RUMBLE_PLAYER_DESTROYED = 20;
	private int   RUMBLE_MAX = 50;

	private float PARTICLE_LIFETIME = 1.3f;
	private float PARTICLE_SPEED = 5;
	private float PARTICLE_MAX_RADIUS = 1;
	private float PARTICLE_SCATTER = 2;
	private float PARTICLES_PER_PX = 0.1f;
	private float PARTICLES_MAX_MULT = 4;
	private int   PARTICLES_PER_POINT = 5;
	private int   PARTICLES_FOR_BIG_EXPLOSION = 50;

	private float POWER_INIT = 250;
	private float POWER_MAX = 250;

	private float POWER_RENEWAL_SPEED = 75; // pixels per second

	private float HEALTH_MAX = 100;
	private float HEALTH_RENEWAL_SPEED = 1;

	private float ENEMY_DAMAGE = 10;
	private float ENEMY_SPEED = 25; // pixels per second
	private float ENEMY_MAX_SPEED = 250;
	private float ENEMY_SPEED_ACC = 2f; // pixels per second^2
	private float ENEMY_PERIOD = 2f;
	private float ENEMY_MIN_PERIOD = 0.75f;
	private float ENEMY_PERIOD_ACC = -0.025f;
	
	private float EXPLOSION_TIME = 2;

	// Looks
	private int UI_BAR_HEIGHT = 5;
	private int PLAYER_CIRCLE_RADIUS = 20;
	private int ENEMY_LENGTH = 30;
	private int PLAYER_HEALTH_GLOW_SIZE = 35;

	private RectF mScratchRect = new RectF(0, 0, 0, 0);

	// Paints
	private int[] mGoodHealth = { 120, 180, 0 };
	private int[] mBadHealth = { 215, 5, 5 };

	private Paint mBgColor;
	private Paint mStarPaint;
	private Paint mPlayerPaint;
	private Paint mUIPowerPaint;
	private Paint mUIPowerGhostPaint;
	private Paint mDrawWallPaint;
	private Paint mFakeWallPaint;
	private Paint mWallPaint;
	private Paint mEnemyPaint;
	private Paint mExplosionPaint;

	// ~~~~~
	// State
	// ~~~~~
	private SurfaceHolder mSurfaceHolder;
	private Handler mHandler;
	private Context mContext;
	private boolean mRun;
	private int mMode;

	private int mCanvasWidth;
	private int mCanvasHeight;

	private int mRumbleAmount;

	private boolean mAlarm;
	private float mAlarmLevel;
	
	private boolean mExploding;
	private float mExplosionLast;

	private float mPower;
	private float mPowerGhost;
	private float mHealth;

	private double mOverallTime;
	private long mWallsMade;

	private long mLastTime;
	private long mLastTimeEnemySpawned;

	private int mX;
	private int mY;

	// Wall drawing
	volatile SparseArray<MotionEvent> mStarted = new SparseArray<MotionEvent>();
	volatile SparseArray<MotionEvent> mMoving = new SparseArray<MotionEvent>();
	volatile SparseArray<MotionEvent> mEnding = new SparseArray<MotionEvent>();

	volatile SparseArray<Point> mStartingPoint = new SparseArray<Point>();
	volatile SparseArray<Point> mCurrentPoint = new SparseArray<Point>();
	volatile SparseArray<Point> mEndingPoint = new SparseArray<Point>();

	volatile SparseArray<Line> mCurrentLines = new SparseArray<Line>();
	volatile SparseArray<Line> mFakeLines = new SparseArray<Line>();

	// Walls
	volatile List<Wall> mWalls = new ArrayList<Wall>();

	// Enemies
	volatile List<Enemy> mEnemies = new ArrayList<Enemy>();

	// Particles
	volatile List<Particle> mParticles = new ArrayList<Particle>();

	// Stars
	private List<Star> mStars = new ArrayList<Star>();
	private Paint mAlarmPaint;

	// Classes
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

	class Line {
		Point a;
		Point b;

		public Line() {
		};

		public Line(Point a, Point b) {
			this.a = a;
			this.b = b;
		}
	}

	class Star extends Point {
		Paint paint;
		float radius;
	}

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

	class Particle {
		Paint paint;
		Point point;
		float radius;
		float life;
		boolean visible;
		float dx;
		float dy;

		public Particle(Point point, Paint paint, float maxRadius) {
			this.point = new Point(point);
			this.paint = new Paint(paint);
			this.radius = 1 + (float) Math.random() * (maxRadius);
			this.life = 0;
			this.visible = true;
			this.dx = 0;
			this.dy = 0;
		}
	}

	// ~~~~
	// Init
	// ~~~~
	public GameThread(SurfaceHolder surfaceHolder, Context context,
			Handler handler) {
		mSurfaceHolder = surfaceHolder;
		mHandler = handler;
		mContext = context;

		// Resources res = context.getResources();

		// Initialize paints
		mBgColor = new Paint();
		mBgColor.setAntiAlias(true);
		mBgColor.setARGB(0, 0, 0, 0);

		mAlarmPaint = new Paint();
		mAlarmPaint.setAntiAlias(true);
		mAlarmPaint.setARGB(255, 220, 10, 10);

		mStarPaint = new Paint();
		mStarPaint.setAntiAlias(true);
		mStarPaint.setARGB(255, 255, 255, 255);

		mUIPowerPaint = new Paint();
		mUIPowerPaint.setAntiAlias(true);
		mUIPowerPaint.setARGB(255, 255, 255, 255);

		mUIPowerGhostPaint = new Paint();
		mUIPowerGhostPaint.setAntiAlias(true);
		mUIPowerGhostPaint.setARGB(64, 255, 255, 255);

		mPlayerPaint = new Paint();
		mPlayerPaint.setAntiAlias(true);
		mPlayerPaint.setARGB(255, 255, 255, 255);

		mDrawWallPaint = new Paint();
		mDrawWallPaint.setAntiAlias(true);
		mDrawWallPaint.setStrokeWidth(3);
		mDrawWallPaint.setARGB(255, 255, 255, 255);

		mWallPaint = new Paint();
		mWallPaint.setAntiAlias(true);
		mWallPaint.setStrokeWidth(3);
		mWallPaint.setARGB(255, 255, 255, 255);

		mFakeWallPaint = new Paint();
		mFakeWallPaint.setAntiAlias(true);
		mFakeWallPaint.setStrokeWidth(1);
		mFakeWallPaint.setARGB(128, 255, 255, 255);

		mEnemyPaint = new Paint();
		mEnemyPaint.setAntiAlias(true);
		mEnemyPaint.setStrokeWidth(3);
		mEnemyPaint.setARGB(255, 220, 10, 10);
		
		mExplosionPaint = new Paint();
		mExplosionPaint.setAntiAlias(true);
		mExplosionPaint.setARGB(255, 220, 200, 200);
	}

	public void doStart() {
		synchronized (mSurfaceHolder) {
			mOverallTime = 0;
			mLastTime = System.currentTimeMillis() + 100;
			mLastTimeEnemySpawned = mLastTime;

			// Health
			mHealth = HEALTH_MAX;

			// Pos
			mX = mCanvasWidth / 2;
			mY = mCanvasHeight / 2;
			mPower = POWER_INIT;

			// Rumble
			mRumbleAmount = 0;

			mWallsMade = 0;

			makeStars();
			setState(STATE_RUNNING);
		}
	}

	public void pause() {
		synchronized (mSurfaceHolder) {
			if (mMode == STATE_RUNNING) {
				setState(STATE_PAUSE);
			}
		}
	}

	public void unpause() {
		// Move the real time clock up to now
		synchronized (mSurfaceHolder) {
			mLastTime = System.currentTimeMillis() + 100;
			mLastTimeEnemySpawned = mLastTime;
		}
		setState(STATE_RUNNING);
	}

	public void setRunning(boolean b) {
		mRun = b;
	}

	@Override
	public void run() {
		while (mRun) {
			Canvas c = null;
			try {
				c = mSurfaceHolder.lockCanvas(null);
				synchronized (mSurfaceHolder) {
					update();
					doDraw(c);
				}
			} finally {
				if (c != null) {
					mSurfaceHolder.unlockCanvasAndPost(c);
				}
			}
		}
	}

	private void setState(int mode) {
		synchronized (mSurfaceHolder) {
			setState(mode, null);
		}
	}

	public void setState(int mode, CharSequence message) {
		/*
		 * This method optionally can cause a text message to be displayed to
		 * the user when the mode changes. Since the View that actually renders
		 * that text is part of the main View hierarchy and not owned by this
		 * thread, we can't touch the state of that View. Instead we use a
		 * Message + Handler to relay commands to the main thread, which updates
		 * the user-text View.
		 */
		synchronized (mSurfaceHolder) {
			mMode = mode;

			if (mMode == STATE_RUNNING) {
				// Message msg = mHandler.obtainMessage();
				// mHandler.sendMessage(msg);
			} else {
				if (mMode == STATE_READY) {

				} else if (mMode == STATE_PAUSE) {

				} else if (mMode == STATE_LOSE) {
					
				}
			}
		}
	}
	
	public void endGame() {
		setRunning(false);
		
		Message msg = mHandler.obtainMessage();
		Bundle data = new Bundle();
		data.putLong("score", calculateScore());
		msg.setData(data);
		mHandler.sendMessage(msg);
	}

	/* Callback invoked when the surface dimensions change. */
	public void setSurfaceSize(int width, int height) {
		// synchronized to make sure these all change atomically
		synchronized (mSurfaceHolder) {
			mCanvasWidth = width;
			mCanvasHeight = height;
		}
	}

	private void doDraw(Canvas canvas) {
		// Clear canvas
		canvas.drawColor(mBgColor.getColor(), PorterDuff.Mode.CLEAR);
		canvas.save();

		// Rumble
		if (mRumbleAmount > 0) {
			canvas.translate(
					(float) Math.floor(Math.random() * (mRumbleAmount / 4 + 1))
							- mRumbleAmount / 8,
					(float) Math.floor(Math.random() * (mRumbleAmount / 4)
							- mRumbleAmount / 8));
		}

		// Stars
		for (int i = 0; i < mStars.size(); ++i) {
			Star star = mStars.get(i);
			canvas.drawCircle(star.x, star.y, star.radius, star.paint);
		}

		if (mMode == STATE_RUNNING) {
			// Health indicator
			canvas.drawCircle(mX, mY, PLAYER_CIRCLE_RADIUS
					+ PLAYER_HEALTH_GLOW_SIZE, makeHealthGradient());
	
			// Draw player
			canvas.drawCircle(mX, mY, PLAYER_CIRCLE_RADIUS, mPlayerPaint);
		}

		// Particles
		Iterator<Particle> particleIter = mParticles.iterator();
		while (particleIter.hasNext()) {
			Particle particle = particleIter.next();
			if (particle.visible) {
				canvas.drawCircle(particle.point.x, particle.point.y,
						particle.radius, particle.paint);
			} else {
				particleIter.remove();
			}
		}

		// Enemies
		Iterator<Enemy> enemyIter = mEnemies.iterator();
		while (enemyIter.hasNext()) {
			Enemy enemy = enemyIter.next();
			if (enemy.visible) {
				canvas.drawLine(enemy.a.x, enemy.a.y, enemy.b.x, enemy.b.y,
						mEnemyPaint);
			} else {
				enemyIter.remove();
			}
		}

		// Walls
		Iterator<Wall> wallIter = mWalls.iterator();
		while (wallIter.hasNext()) {
			Wall wall = wallIter.next();
			if (wall.visible) {
				canvas.drawLine(wall.a.x, wall.a.y, wall.b.x, wall.b.y,
						mWallPaint);
			} else {
				wallIter.remove();
			}
		}

		// Player draws some walls
		for (int i = 0; i < mCurrentLines.size(); ++i) {
			Line line = mCurrentLines.valueAt(i);
			canvas.drawLine(line.a.x, line.a.y, line.b.x, line.b.y,
					mDrawWallPaint);
		}
		for (int i = 0; i < mFakeLines.size(); ++i) {
			Line line = mFakeLines.valueAt(i);
			canvas.drawLine(line.a.x, line.a.y, line.b.x, line.b.y,
					mFakeWallPaint);
		}

		// Draw the power gauge
		if (mMode == STATE_RUNNING) {
			int fuelWidth = (int) ((mCanvasWidth - 8) * mPower / POWER_MAX);
			mScratchRect.set(4, 4, 4 + fuelWidth, 4 + UI_BAR_HEIGHT);
			canvas.drawRect(mScratchRect, mUIPowerPaint);
		}

		// Power gauge ghost
		int fuelGhostWidth = (int) ((mCanvasWidth - 8) * mPowerGhost / POWER_MAX);
		mScratchRect.set(4, 4, 4 + (int) (mCanvasWidth - 8), 4 + UI_BAR_HEIGHT);
		canvas.drawRect(mScratchRect, mUIPowerGhostPaint);

		// Alarm
		if (mAlarm) {
			mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
			canvas.drawRect(mScratchRect, mAlarmPaint);
		}
		
		// Explosion
		if (mExploding) {
			mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
			canvas.drawRect(mScratchRect, mExplosionPaint);
		}
	}

	private synchronized void update() {
		long now = System.currentTimeMillis();

		// Do nothing if mLastTime is in the future.
		// This allows the game-start to delay the start of the physics
		// by 100ms or whatever.
		if (mLastTime > now) {
			return;
		}
		double elapsed = (now - mLastTime) / 1000.0;
		if (mMode == STATE_RUNNING) {
			mOverallTime += elapsed;
		}

		updateRumble(elapsed);
		updateParticles(elapsed);
		updateExplosion(elapsed);
		
		if (mMode == STATE_RUNNING) {
			// Check health
			if (mHealth <= 0) {
				shake(RUMBLE_PLAYER_DESTROYED);
				setState(STATE_LOSE);
				makeExplosion();
				Log.d("Game", "Game over");
			}
	
			// Player draws some walls
			// TODO: Fix occasional NullPointerExceptions
			// Occuring when drawing too fast
			try {
				for (int i = 0; i < mStarted.size(); ++i) {
					MotionEvent e = mStarted.get(i);
					updateStartedDrawingWall(e);
					mStarted.remove(i);
				}
	
				for (int i = 0; i < mMoving.size(); ++i) {
					MotionEvent e = mMoving.get(i);
					updateDrawingWall(e);
				}
	
				for (int i = 0; i < mEnding.size(); ++i) {
					MotionEvent e = mEnding.get(i);
					updateEndingDrawingWall(e);
					mMoving.remove(i);
					mEnding.remove(i);
				}
	
			} catch (NullPointerException e) {
				Log.e("Desync", "Null pointer exception caught. ");
				e.printStackTrace();
	
				// Quick fix
				mStarted.clear();
				mMoving.clear();
				mEnding.clear();
				mCurrentLines.clear();
				mFakeLines.clear();
				mStartingPoint.clear();
				mCurrentPoint.clear();
				mEndingPoint.clear();
			}
	
			// Spawn enemies
			int secondsFromLastSpawn = (int) Math
					.floor((double) (now - mLastTimeEnemySpawned) / 1000);
			int toSpawn = (int) Math.floor(secondsFromLastSpawn / ENEMY_PERIOD);
			if (toSpawn > 0) {
				for (int i = 0; i < toSpawn; ++i) {
					spawnEnemy();
				}
				mLastTimeEnemySpawned = now;
			}
	
			updateDifficulty(elapsed);
			updateEnemies(elapsed);
			
			updateAlarm();
	
			// Restore health and power
			updateHealth(HEALTH_RENEWAL_SPEED * (float) elapsed);
			updatePower(POWER_RENEWAL_SPEED * (float) elapsed);
		}

		mLastTime = now;
	}
	
	private void updateAlarm() {
		if (mHealth / HEALTH_MAX < ALARM_LEVEL) {
			mAlarm = true;
			mAlarmLevel = 1 - mHealth / (HEALTH_MAX * ALARM_LEVEL);
			mAlarmPaint.setAlpha(Math.round(mAlarmLevel * 64));
		} else {
			mAlarm = false;
		}
	}

	private void updateParticles(double elapsed) {
		for (int i = 0; i < mParticles.size(); ++i) {
			Particle particle = mParticles.get(i);
			particle.life += elapsed;
			float lived = particle.life / PARTICLE_LIFETIME;
			if (lived < 1) {
				Paint paint = new Paint(particle.paint);
				paint.setAlpha((int) Math.floor((1 - lived) * 255));
				particle.paint = paint;
				particle.point.x += PARTICLE_SPEED * particle.dx * elapsed;
				particle.point.y += PARTICLE_SPEED * particle.dy * elapsed;
			} else {
				particle.visible = false;
			}
		}
	}

	private void updateRumble(double elapsed) {
		mRumbleAmount -= elapsed;
		if (mRumbleAmount <= 0) {
			mRumbleAmount = 0;
		} else if (mRumbleAmount > RUMBLE_MAX) {
			mRumbleAmount = RUMBLE_MAX;
		}
	}

	private void shake(float amount) {
		mRumbleAmount += amount;
	}

	private void updateDifficulty(double elapsed) {
		ENEMY_PERIOD += ENEMY_PERIOD_ACC * (float) elapsed;
		if (ENEMY_PERIOD < ENEMY_MIN_PERIOD) {
			ENEMY_PERIOD = ENEMY_MIN_PERIOD;
		}
		ENEMY_SPEED += ENEMY_SPEED_ACC * (float) elapsed;
		if (ENEMY_SPEED > ENEMY_MAX_SPEED) {
			ENEMY_SPEED = ENEMY_MAX_SPEED;
		}
	}

	private synchronized void updateStartedDrawingWall(MotionEvent e) {
		int activePointerId = 0;

		Point startPoint = new Point();
		startPoint.x = (int) e.getX();
		startPoint.y = (int) e.getY();

		mStartingPoint.put(activePointerId, startPoint);
		mCurrentPoint.put(activePointerId, startPoint);
	}

	private synchronized void updateEndingDrawingWall(MotionEvent e) {
		int activePointerId = 0;

		Point startingPoint = mStartingPoint.get(activePointerId);
		Point endingPoint = mEndingPoint.get(activePointerId);
		if (endingPoint == null) {
			endingPoint = new Point();
			endingPoint.x = (int) e.getX();
			endingPoint.y = (int) e.getY();
		}

		usePower(metrics(startingPoint, endingPoint));
		makeWall(startingPoint, endingPoint);

		mCurrentLines.remove(activePointerId);
		mFakeLines.remove(activePointerId);
		mStartingPoint.remove(activePointerId);
		mCurrentPoint.remove(activePointerId);
		mEndingPoint.remove(activePointerId);
	}

	private synchronized void updateDrawingWall(MotionEvent e) {
		int activePointerId = 0;

		Point startingPoint = mStartingPoint.get(activePointerId);
		Point currentPoint = new Point();
		currentPoint.x = (int) e.getX();
		currentPoint.y = (int) e.getY();
		mCurrentPoint.put(activePointerId, currentPoint);

		Point endingPoint = mEndingPoint.get(activePointerId);
		int currpx = Math.round(metrics(startingPoint, currentPoint));

		float length = mPower;

		if (length < currpx) {
			float k = length / currpx;
			endingPoint = new Point();
			endingPoint.x = startingPoint.x
					+ (int) Math.round(k * (currentPoint.x - startingPoint.x));
			endingPoint.y = startingPoint.y
					+ (int) Math.round(k * (currentPoint.y - startingPoint.y));
			mEndingPoint.put(activePointerId, endingPoint);

			makeDrawWall(activePointerId, startingPoint, endingPoint);
			makeFakeWall(activePointerId, endingPoint, currentPoint);
		} else {
			mFakeLines.remove(activePointerId);
			mEndingPoint.remove(activePointerId);
			makeDrawWall(activePointerId, startingPoint, currentPoint);
		}
	}

	private synchronized void spawnEnemy() {
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
		float ao = metrics(a, o);
		Point b = new Point();
		b.x = a.x - Math.round(ENEMY_LENGTH / ao * (o.x - a.x));
		b.y = a.y - Math.round(ENEMY_LENGTH / ao * (o.y - a.y));

		Enemy enemy = new Enemy();
		enemy.a = a;
		enemy.b = b;

		mEnemies.add(enemy);
	}

	private synchronized void updateEnemies(double elapsed) {
		for (int i = 0; i < mEnemies.size(); ++i) {
			Enemy enemy = mEnemies.get(i);

			// Move
			Point o = new Point(mX, mY);
			Point a = enemy.a;
			float ao = metrics(a, o);
			float dist = (float) (ENEMY_SPEED * elapsed);
			float dx = (dist * (o.x - a.x) / ao);
			float dy = (dist * (o.y - a.y) / ao);

			enemy.a.x += dx;
			enemy.a.y += dy;
			enemy.b.x += dx;
			enemy.b.y += dy;

			// Check if wall is hit
			// TODO: Fix occasional exceptions
			try {
				for (int j = 0; j < mWalls.size(); ++j) {
					Wall wall = mWalls.get(j);
					Point intersection = intersect(wall, enemy);
					if (intersection != null) {
						wall.visible = false;
						makeDebris(wall, mWallPaint);
						enemy.visible = false;
						makeDebris(enemy, mEnemyPaint);
						makeDebris(intersection, mWallPaint, PARTICLES_PER_POINT);
						shake(RUMBLE_WALL_HIT);
						continue;
					}
				}

				// Check if the player is hit
				float distToPlayer = metrics(enemy.a, o);
				if (distToPlayer < PLAYER_CIRCLE_RADIUS) {
					drainHealth(ENEMY_DAMAGE);
					shake(RUMBLE_PLAYER_HIT);
					enemy.visible = false;
					makeDebris(enemy, mEnemyPaint);
				}

				// Quick fix
			} catch (IndexOutOfBoundsException e) {
				Log.e("Desync", "OutOfBoundsException caught");
				e.printStackTrace();
			}
		}
	}

	private synchronized void updateHealth(float points) {
		float restored = mHealth + points;
		if (restored > HEALTH_MAX) {
			mHealth = HEALTH_MAX;
		} else {
			mHealth = restored;
		}
	}

	private void drainHealth(float damage) {
		updateHealth(-damage);
	}

	private void updatePower(float amount) {
		float updated = mPower + amount;
		if (amount < 0) {
			mPowerGhost = mPower;
		}
		if (updated <= 0) {
			mPower = 0;
		} else if (updated > POWER_MAX) {
			mPower = POWER_MAX;
		} else {
			mPower = updated;
		}
	}

	private void usePower(float amount) {
		updatePower(-amount);
	}

	private void makeStars() {
		for (int i = 0; i < STARS; ++i) {
			Star star = new Star();
			star.x = (float) Math.random() * (mCanvasWidth + 1);
			star.y = (float) Math.random() * (mCanvasHeight + 1);
			star.radius = (float) Math.random() * (STAR_MAX_RADIUS + 1);
			star.paint = new Paint(mStarPaint);
			star.paint.setAlpha(1 + (int) Math.round(Math.random() * 128));
			mStars.add(star);
		}
	}

	private void makeDrawWall(int activePointerId, Point a, Point b) {
		Line currentLine = new Line();
		currentLine.a = a;
		currentLine.b = b;
		mCurrentLines.put(activePointerId, currentLine);
	}

	private void makeFakeWall(int activePointerId, Point a, Point b) {
		Line fakeLine = new Line();
		fakeLine.a = a;
		fakeLine.b = b;
		mFakeLines.put(activePointerId, fakeLine);
	}

	private void makeWall(Point a, Point b) {
		Wall wall = new Wall();
		wall.a = a;
		wall.b = b;
		mWallsMade++;
		mWalls.add(wall);
	}

	private void makeDebris(Line line, Paint paint) {
		float length = metrics(line.a, line.b);
		int count = Math.round(length * PARTICLES_PER_PX);
		float part = length / count;
		float dx = part / length * (line.b.x - line.a.x);
		float dy = part / length * (line.b.y - line.a.y);
		for (int i = 0; i < count; ++i) {
			Point debris = new Point();
			debris.x = line.a.x + dx * i + randsgn() * (float) Math.random()
					* PARTICLE_SCATTER;
			debris.y = line.a.y + dy * i + randsgn() * (float) Math.random()
					* PARTICLE_SCATTER;
			Particle particle = new Particle(debris, paint, PARTICLE_MAX_RADIUS);
			particle.dx = randsgn() * (float) Math.random() * 1;
			particle.dy = randsgn() * (float) Math.random() * 1;
			mParticles.add(particle);
		}
	}

	private void makeDebris(Point point, Paint paint, int numParticles) {
		for (int i = 0; i < numParticles; ++i) {
			Point debris = new Point(point);
			debris.x += randsgn() * Math.random() * PARTICLE_SCATTER;
			debris.y += randsgn() * Math.random() * PARTICLE_SCATTER;
			Particle particle = new Particle(debris, paint, PARTICLE_MAX_RADIUS);
			particle.dx = randsgn() * (float) Math.random()
					* PARTICLES_MAX_MULT;
			particle.dy = randsgn() * (float) Math.random()
					* PARTICLES_MAX_MULT;
			mParticles.add(particle);
		}
	}
	
	private void makeExplosion() {
		mExploding = true;
		mAlarm = false;
		mExplosionLast = 0;
		makeDebris(new Point(mX, mY), mPlayerPaint, PARTICLES_FOR_BIG_EXPLOSION);
		makeDebris(new Point(mX, mY), mEnemyPaint, PARTICLES_FOR_BIG_EXPLOSION);
		
		mStarted.clear();
		mMoving.clear();
		mEnding.clear();
		mStartingPoint.clear();
		mCurrentPoint.clear();
		mEndingPoint.clear();
		mCurrentLines.clear();
		mFakeLines.clear();
		mWalls.clear();
		mEnemies.clear();
	}
	
	private void updateExplosion(double elapsed) {
		if (mExploding) {
			float explosionRatio = 1 - mExplosionLast / EXPLOSION_TIME;
			mExplosionPaint.setAlpha(Math.round(explosionRatio * 255));
			mExplosionLast += elapsed;
			if (mExplosionLast >= EXPLOSION_TIME) {
				endGame();
			}
		}
	}

	private Paint makeHealthGradient() {
		float[] good = new float[3];
		Color.RGBToHSV(mGoodHealth[0], mGoodHealth[1], mGoodHealth[2], good);
		float[] bad = new float[3];
		Color.RGBToHSV(mBadHealth[0], mBadHealth[1], mBadHealth[2], bad);

		float ratio = mHealth / HEALTH_MAX;
		float[] blend = new float[3];
		blend[0] = transition(ratio, good[0], bad[0]);
		blend[1] = transition(ratio, good[1], bad[1]);
		blend[2] = transition(ratio, good[2], bad[2]);

		RadialGradient gradient = new RadialGradient(mX, mY,
				PLAYER_CIRCLE_RADIUS + PLAYER_HEALTH_GLOW_SIZE,
				Color.HSVToColor(blend), 0x00000000,
				android.graphics.Shader.TileMode.CLAMP);

		Paint result = new Paint();
		result.setDither(true);
		result.setShader(gradient);

		return result;
	}

	private long calculateScore() {
		return Math.round(1000 * mOverallTime) - 10 * mWallsMade;
	}

	public synchronized void onTouch(MotionEvent e) {
		final int action = e.getAction() & MotionEvent.ACTION_MASK;
		final int activePointerId = 0;

		if (action == MotionEvent.ACTION_DOWN) {
			mStarted.put(activePointerId, e);
		}
		if (action == MotionEvent.ACTION_MOVE) {
			mMoving.put(activePointerId, e);
		}
		if (action == MotionEvent.ACTION_UP) {
			mEnding.put(activePointerId, e);
		}
	}

	// ~~~~~
	// Utils
	// ~~~~~

	public int randsgn() {
		int r = (int) Math.round(Math.random());
		if (r == 0) {
			return -1;
		} else {
			return 1;
		}
	}

	public float transition(float ratio, float a, float b) {
		return a + (b - a) * (1 - ratio);
	}

	private float metrics(Point a, Point b) {
		float xs = b.x - a.x;
		float ys = b.y - a.y;
		return (float) (Math.sqrt(xs * xs + ys * ys));
	}

	// (c) Alexander Hristov
	// http://www.ahristov.com/tutorial/geometry-games/intersection-segments.html
	public Point intersect(Line a, Line b) {
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
