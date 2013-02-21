package com.kulinich.tapdefence;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.kulinich.tapdefence.engine.GameSurface;
import com.kulinich.tapdefence.engine.GameThread;

@SuppressLint("NewApi")
public class GameActivity extends FragmentActivity {
	
    private GameThread mGameThread;
    private GameSurface mGameSurface;
    
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
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
		
		mGameSurface = new GameSurface(this);
		
		setContentView(mGameSurface);
		
		mGameThread = mGameSurface.getThread();
		mGameThread.setSurfaceSize(width, height);
		mGameThread.doStart();
	}	
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if (mGameThread != null) {
			mGameThread.pause();
		}
	}

	public void showScoreDialog(long score) {
	    ScoreDialog scoreDialog = new ScoreDialog();
	    scoreDialog.score = score;
	    scoreDialog.show(getSupportFragmentManager(), "scoreDialog");
	}
}
