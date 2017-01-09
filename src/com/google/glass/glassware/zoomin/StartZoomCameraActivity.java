package com.google.glass.glassware.zoomin;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class StartZoomCameraActivity extends Activity implements BaseListener{

	public 		static		String	 				TAG 			= PerformZoomActivity.TAG;
	private 				TextView 				mTextView		= null; 
	private 				GlassSliderView			mSliderView		= null;
	private 				GestureDetector 		mGestureDetector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Use TuggableView
		setContentView(new GlassTuggableView(this, R.layout.start_zoom_camera ));
		
		// announce;
		announce();
		
		if (getIntent().getBooleanExtra("PLEASE_EXIT", false)) {   
			Log.i(TAG,"[INFO] StartZoomCameraActivity Identified an exit request. exiting... " );
			finish();
		}

		// Keep Glass Awake.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Initiate the Gesture Detector.
		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(this);

		// Start the indeterminate Slider.
		mSliderView = (GlassSliderView) findViewById(R.id.slider);
		mSliderView.setVisibility(View.VISIBLE);
		mSliderView.startIndeterminate();

		// wait 2.5 seconds, than start the PerformZoomActivity 
		Thread thread = new Thread() {

			public void run() {
				int timer = 0;
				try {
					while (timer < 1750) {
						sleep(250);
						timer = timer + 250;
					}

					// starting
					startActivity(new Intent("android.intent.action.PERFORM_ZOOM_ACTIVITY"));
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					finish();
				}
			}
		};
		thread.start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra("PLEASE_EXIT", false)) {   
			Log.i(TAG,"[INFO] StartZoomCameraActivity Identified an exit request. exiting... " );
			finish();
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			onBackPressed();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

		//Announce the cancellation 
		mTextView.setText("       Canceling...");

		// Wait 
		Thread thread = new Thread() {
			public void run() {
				int timer = 0;
				try {
					while (timer < 1500) {
						sleep(250);
						timer = timer + 250;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					finish();
				}
			}
		};
		thread.start();
		Log.i(TAG, "[INFO] onBackPressed is called.");
		exitGlassware();
	}

	/**
	 * Exit.
	 */
	private void exitGlassware() {
		Log.i(TAG, "[INFO] User selected to exit glassware.");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra("PLEASE_EXIT", true);
		startActivity(intent);
	}
	
	/**
	 * Announce.
	 */
	private void announce() {
		
		Log.i(TAG, "[INFO] ****************************");
		Log.i(TAG, "[INFO] ***   Launched Magnify   ***");
		Log.i(TAG, "[INFO] ****************************");
		
		getBatteryLevel();
	}
	
	/**
	 * Get Battery Level. 
	 **/
	public void getBatteryLevel() {
		Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		// Error checking that probably isn't needed but I added just in case.
		if(level == -1 || scale == -1) {
			Log.e(TAG, "[INFO] Failed to check the battery level");
			return;
		}

		float batteryLevel = ((float)level / (float)scale) * 100.0f;;

		Log.i(TAG, "[INFO] Battery Level during launch is: " +batteryLevel);
		return; 
	}
	
	/*
	 * Send generic motion events to the gesture detector
	 */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {

		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	@Override
	public boolean onGesture(Gesture gesture) {
		AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		if (gesture == Gesture.TAP) {
			Log.i(TAG, "[INFO] User tapped. Play the disallowed sound.");
			audio.playSoundEffect(Sounds.DISALLOWED);
			return true;
		}
		return false;
	}
}
