package com.google.glass.glassware.zoomin;

import android.os.SystemClock;
import android.view.SurfaceHolder;
import com.google.android.glass.timeline.DirectRenderingCallback;

public class ZoominLiveCardRenderer implements DirectRenderingCallback {

	private 				SurfaceHolder 	mHolder;
	private 				boolean 		mPaused;
	private 				RenderThread 	mRenderThread;
	private static final 	long 			FRAME_TIME_MILLIS = 33;

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// Update your views accordingly.
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		updateRendering();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHolder = null;
		updateRendering();
	}

	@Override
	public void renderingPaused(SurfaceHolder holder, boolean paused) {
		mPaused = paused;
		updateRendering();
	}

	/**
	 * Start or stop rendering according to the timeline state.
	 */
	private synchronized void updateRendering() {
		boolean shouldRender = (mHolder != null) && !mPaused;
		boolean rendering = mRenderThread != null;

		if (shouldRender != rendering) {
			if (shouldRender) {
				mRenderThread = new RenderThread();
				mRenderThread.start();
			} else {
				mRenderThread.quit();
				mRenderThread = null;
			}
		}
	}

	/**
	 * Redraws in the background.
	 */
	private class RenderThread extends Thread {
		private boolean mShouldRun;

		/**
		 * Initializes the background rendering thread.
		 */
		public RenderThread() {
			mShouldRun = true;
		}

		/**
		 * Returns true if the rendering thread should continue to run.
		 */
		private synchronized boolean shouldRun() {
			return mShouldRun;
		}

		/**
		 * Requests that the rendering thread exit at the next opportunity.
		 */
		public synchronized void quit() {
			mShouldRun = false;
		}

		@Override
		public void run() {
			while (shouldRun()) {
				//draw();
				SystemClock.sleep(FRAME_TIME_MILLIS);
			}
		}
	}
}