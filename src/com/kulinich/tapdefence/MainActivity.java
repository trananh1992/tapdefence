package com.kulinich.tapdefence;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import com.kulinich.tapdefence.graphics.GameSurface;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
	
    /** A handle to the thread that's actually running the animation. */
    private GameSurface.GameThread mGameThread;

    /** A handle to the View in which the game is running. */
    private GameSurface mGameSurface;

	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mGameSurface = new GameSurface(this);
		setContentView(mGameSurface);
		
		// Get screen resolution
		Display display = getWindowManager().getDefaultDisplay();
		int width, height;
		if (android.os.Build.VERSION.SDK_INT < 13) {
			width = display.getWidth();
			height = display.getHeight();
		} else {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
			height = size.y;	
		}
		
		mGameThread = mGameSurface.getThread();
		mGameThread.setSurfaceSize(width, height);
		mGameThread.doStart();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mGameThread.pause();
	}
}
