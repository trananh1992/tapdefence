package com.kulinich.tapdefence.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.kulinich.tapdefence.GameActivity;

public class GameSurface extends SurfaceView implements SurfaceHolder.Callback {

	SurfaceHolder holder;
	GameThread thread;

	public GameSurface(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);

		thread = new GameThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
				if (m.getData() != null) {
					GameActivity activity = (GameActivity) getContext();
					long score = m.getData().getLong("score");
					activity.showScoreDialog(score);
				}
			}
		});

		setFocusable(true);
	}
	
	public GameThread getThread() {
		return thread;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		thread.setSurfaceSize(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		thread.setRunning(true);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread.setRunning(false);
		boolean retry = true;
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}
	
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		thread.onTouch(e);
		return true;
	}

}
