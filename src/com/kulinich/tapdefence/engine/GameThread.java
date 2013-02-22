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

	private Game mGame;
	private InputHandler mInput;

	private RectF mScratchRect = new RectF(0, 0, 0, 0);

	SurfaceHolder mSurfaceHolder;
	Handler mHandler;
	Context mContext;

	int mCanvasWidth;
	int mCanvasHeight;

	boolean mRun;
	int mMode;

	public GameThread(SurfaceHolder surfaceHolder, Context context,
			Handler handler) {
		mSurfaceHolder = surfaceHolder;
		mHandler = handler;
		mContext = context;

		// Resources res = context.getResources();

		Constants.initializePaints();
	}

	public void doStart() {
		synchronized (mSurfaceHolder) {
			mGame = new Game(this);
			mInput = new InputHandler(mGame);
		}
		setState(Constants.STATE_RUNNING);
	}

	public void pause() {
		synchronized (mSurfaceHolder) {
			if (mMode == Constants.STATE_RUNNING) {
				setState(Constants.STATE_PAUSE);
			}
		}
	}

	public void unpause() {
		// Move the real time clock up to now
		synchronized (mSurfaceHolder) {
			mGame.mLastTime = System.currentTimeMillis() + 100;
			mGame.mLastTimeEnemySpawned = mGame.mLastTime;
		}
		setState(Constants.STATE_RUNNING);
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
					if (mMode == Constants.STATE_RUNNING
							|| mMode == Constants.STATE_LOSE) {
						update();
						doDraw(c);
					}
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

			if (mMode == Constants.STATE_RUNNING) {
				// Message msg = mHandler.obtainMessage();
				// mHandler.sendMessage(msg);
			} else {
				if (mMode == Constants.STATE_READY) {

				} else if (mMode == Constants.STATE_PAUSE) {

				} else if (mMode == Constants.STATE_LOSE) {

				}
			}
		}
	}

	public void endGame() {
		setRunning(false);

		Message msg = mHandler.obtainMessage();
		Bundle data = new Bundle();
		data.putLong("score", mGame.calculateScore());
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
		canvas.drawColor(Constants.BG_COLOR_PAINT.getColor(),
				PorterDuff.Mode.CLEAR);
		canvas.save();

		// Rumble
		if (mGame.mRumbleAmount > 0) {
			canvas.translate(
					(float) Math.floor(Math.random()
							* (mGame.mRumbleAmount / 4 + 1))
							- mGame.mRumbleAmount / 8,
					(float) Math.floor(Math.random()
							* (mGame.mRumbleAmount / 4) - mGame.mRumbleAmount
							/ 8));
		}

		// Stars
		for (int i = 0; i < mGame.mStars.size(); ++i) {
			Star star = mGame.mStars.get(i);
			canvas.drawCircle(star.x, star.y, star.radius, star.paint);
		}

		if (mMode == Constants.STATE_RUNNING) {
			// Health indicator
			canvas.drawCircle(mGame.mX, mGame.mY,
					Constants.PLAYER_CIRCLE_RADIUS
							+ Constants.PLAYER_HEALTH_GLOW_SIZE,
					mGame.makeHealthGradient());

			// Draw player
			canvas.drawCircle(mGame.mX, mGame.mY,
					Constants.PLAYER_CIRCLE_RADIUS, Constants.PLAYER_PAINT);
		}

		// Particles
		Iterator<Particle> particleIter = mGame.mParticles.iterator();
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
		Iterator<Enemy> enemyIter = mGame.mEnemies.iterator();
		while (enemyIter.hasNext()) {
			Enemy enemy = enemyIter.next();
			if (enemy.visible) {
				canvas.drawLine(enemy.a.x, enemy.a.y, enemy.b.x, enemy.b.y,
						Constants.ENEMY_PAINT);
			} else {
				enemyIter.remove();
			}
		}

		// Walls
		Iterator<Wall> wallIter = mGame.mWalls.iterator();
		while (wallIter.hasNext()) {
			Wall wall = wallIter.next();
			if (wall.visible) {
				canvas.drawLine(wall.a.x, wall.a.y, wall.b.x, wall.b.y,
						Constants.WALL_PAINT);
			} else {
				wallIter.remove();
			}
		}

		// Player draws some walls
		for (int i = 0; i < mInput.mCurrentLines.size(); ++i) {
			Line line = mInput.mCurrentLines.valueAt(i);
			canvas.drawLine(line.a.x, line.a.y, line.b.x, line.b.y,
					Constants.DRAW_WALL_PAINT);
		}
		for (int i = 0; i < mInput.mFakeLines.size(); ++i) {
			Line line = mInput.mFakeLines.valueAt(i);
			canvas.drawLine(line.a.x, line.a.y, line.b.x, line.b.y,
					Constants.FAKE_WALL_PAINT);
		}

		// Draw the power gauge
		if (mMode == Constants.STATE_RUNNING) {
			int fuelWidth = (int) ((mCanvasWidth - 8) * mGame.mPower / Constants.POWER_MAX);
			mScratchRect.set(4, 4, 4 + fuelWidth, 4 + Constants.UI_BAR_HEIGHT);
			canvas.drawRect(mScratchRect, Constants.UI_POWER_PAINT);
		}

		// Power gauge ghost
		int fuelGhostWidth = (int) ((mCanvasWidth - 8) * mGame.mPowerGhost / Constants.POWER_MAX);
		mScratchRect.set(4, 4, 4 + (int) (mCanvasWidth - 8),
				4 + Constants.UI_BAR_HEIGHT);
		canvas.drawRect(mScratchRect, Constants.UI_POWER_GHOST_PAINT);

		// Alarm
		if (mGame.mAlarm) {
			mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
			canvas.drawRect(mScratchRect, Constants.ALARM_PAINT);
		}

		// Explosion
		if (mGame.mExploding) {
			mScratchRect.set(0, 0, mCanvasWidth, mCanvasHeight);
			canvas.drawRect(mScratchRect, Constants.EXPLOSION_PAINT);
		}
	}

	private synchronized void update() {
		long now = System.currentTimeMillis();

		// Do nothing if mLastTime is in the future.
		// This allows the game-start to delay the start of the physics
		// by 100ms or whatever.
		if (mGame.mLastTime > now) {
			return;
		}

		double elapsed = (now - mGame.mLastTime) / 1000.0;
		mGame.updateRumble(elapsed);

		if (mMode == Constants.STATE_RUNNING) {
			mGame.mOverallTime += elapsed;

			// Check health
			if (mGame.mHealth <= 0) {
				mGame.shake(Constants.RUMBLE_PLAYER_DESTROYED);
				setState(Constants.STATE_LOSE);
				mGame.makeExplosion();
				Log.d("Game", "Game over");
			}

			// Player draws some walls
			mInput.update();

			// Spawn enemies
			int secondsFromLastSpawn = (int) Math
					.floor((double) (now - mGame.mLastTimeEnemySpawned) / 1000);
			int toSpawn = (int) Math.floor(secondsFromLastSpawn
					/ mGame.mEnemyPeriod);
			if (toSpawn > 0) {
				for (int i = 0; i < toSpawn; ++i) {
					mGame.spawnEnemy();
				}
				mGame.mLastTimeEnemySpawned = now;
			}

			mGame.updateDifficulty(elapsed);
			mGame.updateEnemies(elapsed);

			mGame.updateAlarm();

			// Restore health and power
			mGame.updateHealth(Constants.HEALTH_RENEWAL_SPEED * (float) elapsed);
			mGame.updatePower(Constants.POWER_RENEWAL_SPEED * (float) elapsed);
		}

		mGame.updateParticles(elapsed);
		mGame.updateExplosion(elapsed);

		mGame.mLastTime = now;
	}

	void onTouch(MotionEvent e) {
		mInput.onTouch(e);
	}
}
