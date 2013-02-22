package com.kulinich.tapdefence.engine;

import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;

public class InputHandler {

	Game mGame;
	
	// TODO: Add multitouch support
	static int activePointerId = 0;
	
	volatile SparseArray<MotionEvent> mStarted = new SparseArray<MotionEvent>();
	volatile SparseArray<MotionEvent> mMoving = new SparseArray<MotionEvent>();
	volatile SparseArray<MotionEvent> mEnding = new SparseArray<MotionEvent>();

	volatile SparseArray<Point> mStartingPoint = new SparseArray<Point>();
	volatile SparseArray<Point> mCurrentPoint = new SparseArray<Point>();
	volatile SparseArray<Point> mEndingPoint = new SparseArray<Point>();

	volatile SparseArray<Line> mCurrentLines = new SparseArray<Line>();
	volatile SparseArray<Line> mFakeLines = new SparseArray<Line>();
	
	InputHandler(Game game) {
		mGame = game;
	}
	
	synchronized void updateStartedDrawingWall(MotionEvent e) {
		Point startPoint = new Point();
		startPoint.x = (int) e.getX();
		startPoint.y = (int) e.getY();

		mStartingPoint.put(activePointerId, startPoint);
		mCurrentPoint.put(activePointerId, startPoint);
	}

	synchronized void updateEndingDrawingWall(MotionEvent e) {
		Point startingPoint = mStartingPoint.get(activePointerId);
		Point endingPoint = mEndingPoint.get(activePointerId);
		if (endingPoint == null) {
			endingPoint = new Point();
			endingPoint.x = e.getX();
			endingPoint.y = e.getY();
		}

		mGame.usePower(Utils.metrics(startingPoint, endingPoint));
		mGame.makeWall(startingPoint, endingPoint);

		mCurrentLines.remove(activePointerId);
		mFakeLines.remove(activePointerId);
		mStartingPoint.remove(activePointerId);
		mCurrentPoint.remove(activePointerId);
		mEndingPoint.remove(activePointerId);
	}

	synchronized void updateDrawingWall(MotionEvent e) {
		Point startingPoint = mStartingPoint.get(activePointerId);
		Point currentPoint = new Point();
		currentPoint.x = e.getX();
		currentPoint.y = e.getY();
		mCurrentPoint.put(activePointerId, currentPoint);

		Point endingPoint = mEndingPoint.get(activePointerId);
		int currpx = Math.round(Utils.metrics(startingPoint, currentPoint));

		float length = mGame.mPower;

		if (length < currpx) {
			float k = length / currpx;
			endingPoint = new Point();
			endingPoint.x = startingPoint.x
					+ Math.round(k * (currentPoint.x - startingPoint.x));
			endingPoint.y = startingPoint.y
					+ Math.round(k * (currentPoint.y - startingPoint.y));
			mEndingPoint.put(activePointerId, endingPoint);

			makeDrawWall(activePointerId, startingPoint, endingPoint);
			makeFakeWall(activePointerId, endingPoint, currentPoint);
		} else {
			mFakeLines.remove(activePointerId);
			mEndingPoint.remove(activePointerId);
			makeDrawWall(activePointerId, startingPoint, currentPoint);
		}
	}

	public void update() {
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
			clear();
		}
	}
	
	public void clear() {
		mStarted.clear();
		mMoving.clear();
		mEnding.clear();
		mStartingPoint.clear();
		mCurrentPoint.clear();
		mEndingPoint.clear();
		mCurrentLines.clear();
		mFakeLines.clear();
	}
	
	synchronized void makeDrawWall(int activePointerId, Point a, Point b) {
		Line currentLine = new Line();
		currentLine.a = a;
		currentLine.b = b;
		mCurrentLines.put(activePointerId, currentLine);
	}

	synchronized void makeFakeWall(int activePointerId, Point a, Point b) {
		Line fakeLine = new Line();
		fakeLine.a = a;
		fakeLine.b = b;
		mFakeLines.put(activePointerId, fakeLine);
	}
	
	synchronized void onTouch(MotionEvent e) {
		final int action = e.getAction() & MotionEvent.ACTION_MASK;

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
}
