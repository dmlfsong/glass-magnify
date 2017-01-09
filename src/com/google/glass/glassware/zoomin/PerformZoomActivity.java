package com.google.glass.glassware.zoomin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.LayoutInflater.Factory;
import android.view.View.MeasureSpec;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;

/**
 * This activity presents the Video Preview and handles zooming in and out. 
 * @author dovik
 *
 */
public class PerformZoomActivity extends Activity  implements BaseListener{

	public  static   		String 				TAG 			 	= "Magnify";
	private static final 	String 				LIVE_CARD_ID	 	= "magnify";
	private 				LiveCard 			mLiveCard;
	private 				GlassCameraPreview 	mPreview			= null;
	public  static 			Camera 				mCamera;
	private 				GestureDetector 	mGestureDetector;
	private static			int					GLASS_MAX_ZOOM 		= 60;
	private 				CountDownTimer 		mDisplayTimer 		= null;
	private 				ProgressBar			mProgressBar    	= null;		
	private 			 	RelativeLayout 		mTipsContainer		= null;	;
	private 			 	TextView 			mTipsView			= null;
	private 				ImageView			mMagnifyingGlass	= null;
	private	static final	int 				ZOOM_IN				= 0;
	private	static final	int 				ZOOM_OUT			= 1;
	private	static final	int 				CANT_ZOOM_IN		= 2;
	private	static final	int 				CANT_ZOOM_OUT		= 3;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
	
		// Use TuggableView
		//setContentView(new GlassTuggableView(this, R.layout.preview_zoom_camera ));
		setContentView(R.layout.preview_zoom_camera );
		
		// Get Camera instance.
		getCameraInstance();
		if(mCamera == null){
			return;
		}

		// Init the LiveCard.
		initLiveCard();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new GlassCameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.removeAllViews();
		preview.addView(mPreview);

		// Get the tips container.
		mTipsContainer = (RelativeLayout) findViewById(R.id.tips_container);
		mTipsView = (TextView)findViewById(R.id.tips_view);


		// Start a 30 sec timer. 
		setOrResetTimer();

		// Initiate the Gesture Detector.
		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(this);

		// prevent Glass from Sleeping.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

	/** 
	 * A safe way to get an instance of the Camera object. 
	 **/
	public static void getCameraInstance(){

		try {
			mCamera = Camera.open(); // attempt to get a Camera instance
		}
		catch (Exception e){
			// Camera is not available (in use or does not exist)
			Log.e(TAG, "[ERROR] The Camera is not available: " + e.getMessage());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "[INFO] onPause called.");

		if (null != mCamera) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			Log.i(TAG, "[INFO] Camera Released!");
			mCamera.release();
			mCamera = null;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "[INFO] onResume Called.");

		// Create an instance of Camera
		if (null == mCamera) {
			getCameraInstance();
		}
		// Create our Preview view and set it as the content of our activity.
		mPreview = new GlassCameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.removeAllViews();
		preview.addView(mPreview);
	}


	/*
	 * Create a new LiveCard.
	 */
	private void initLiveCard() {

		if (mLiveCard == null) {

			mLiveCard = new LiveCard(this, LIVE_CARD_ID);

			// Enable direct rendering.
			mLiveCard.setDirectRenderingEnabled(true);

			// Set callback
			mLiveCard.getSurfaceHolder().addCallback(new ZoominLiveCardRenderer());

			// Set action
			Intent intent = new Intent(this, PerformZoomActivity.class);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0,
					intent, 0));
			mLiveCard.publish(LiveCard.PublishMode.SILENT);

		} else {	
			// LiveCard already exists
			return;
		}
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "[INFO] OnBackPressed was called.");
		super.onBackPressed();
		if (null != mCamera) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			Log.i(TAG, "[INFO] Camera Released.");
			mCamera.release();
			mCamera = null;
		}

		if (mLiveCard.isPublished()) {
			mLiveCard.unpublish();
		}
		exitGlassware();
	}

	/*
	 * Zoom in.
	 */
	private boolean zoomIn(int zoomBy) {

		// validations.
		if (null == mCamera) {
			Log.i(TAG, "[INFO] Unfortunately the Camera Object is invalid.");
			return false;
		}
		if (zoomBy > GLASS_MAX_ZOOM || zoomBy < 0)  {
			Log.i(TAG, "[INFO] Invalid zoom request (" + zoomBy +").");
			return false;
		}		

		int maxZoom = mCamera.getParameters().getMaxZoom();
		int curZoom = mCamera.getParameters().getZoom();

		if (curZoom >= maxZoom) {
			showZoomIndicator(CANT_ZOOM_IN);
			Log.i(TAG, "[INFO] Unfortunately the Camera is at its maximal zoom (maximum: " + maxZoom + ", current: " + curZoom +".");
			return false;
		}

		// Increase zoom.
		curZoom = curZoom + zoomBy;

		if (curZoom > maxZoom) {
			curZoom = maxZoom;
		}

		try {

			showZoomIndicator(ZOOM_IN);

			// Zoom in.
			try {
				mCamera.startSmoothZoom(curZoom);
			} catch (Exception e) {
				Log.e(TAG, "[ERROR] RunTimeException thrown: " + e.getMessage());
				return false;
			}

		} catch (Exception e) {
			Log.e(TAG, "[ERROR] RunTimeException thrown: " + e);
			return false;
		}
		return true;
	}

	/*
	 * Zoom out.
	 */
	private boolean zoomOut(int zoomBy) {

		if (null == mCamera) {
			Log.i(TAG, "[INFO] Unfortunately the Camera Object is invalid.");
			return false;
		}

		if (zoomBy > GLASS_MAX_ZOOM)  {
			showZoomIndicator(CANT_ZOOM_OUT);
			Log.i(TAG, "[INFO] Invalid zoom request (zoomBy is greater than MAX_ZOOM)");
			return false;
		}

		int curZoom = mCamera.getParameters().getZoom();

		if (curZoom <= 0) {
			showZoomIndicator(CANT_ZOOM_OUT);
			Log.i(TAG, "[INFO] Unfortunately the camera cannot be zoomed out any further.");
			return false;
		}

		curZoom-=zoomBy;

		if (curZoom <0 ){
			curZoom = 0;
		}

		// Zoom out
		try {
			showZoomIndicator(ZOOM_OUT);
			mCamera.startSmoothZoom(curZoom);
		} catch (Exception e) {
			Log.e(TAG, "[ERROR] RunTimeException thrown: " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Exit.
	 */
	private void exitGlassware() {
		Log.i(TAG,"[INFO] Exiting Glassware!");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
		startActivity(intent);
		finish();
	}
	
	/**
	 * Fade in and fade out the tips.
	 **/
	private void displayBothTips(long millisUntilFinished) {

		// Display the swipe forward tip. 
		if (millisUntilFinished > 27000 && millisUntilFinished <=28000) {
			mTipsView.setText(getResources().getString(R.string.guidance_swipe_forward));
			Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/Roboto-Light.ttf");
			mTipsView.setTypeface(typeFace);
			mTipsView.setTextSize(TypedValue.COMPLEX_UNIT_PX,26);
			doLayout();
			mTipsContainer.animate().alpha(1.0f).start();
			return;
		}

		// Swipe Forward tip fades away.
		if (millisUntilFinished > 25000 && millisUntilFinished <=26000) {
			mTipsView.setText("");
			doLayout();
			mTipsContainer.animate().alpha(0.0f).start();
			return;
		}

		// Display the swipe backwards tip. 
		if (millisUntilFinished > 24000 && millisUntilFinished <=25000) {
			mTipsView.setText(getResources().getString(R.string.guidance_swipe_back));
			Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/Roboto-Light.ttf");
			mTipsView.setTypeface(typeFace);
			mTipsView.setTextSize(TypedValue.COMPLEX_UNIT_PX,26);
			doLayout();
			mTipsContainer.animate().alpha(1.0f).start();
			return;
		}

		// Swipe backwards tip fades away.
		if (millisUntilFinished > 22000 && millisUntilFinished <=23000) {
			mTipsView.setText("");
			doLayout();
			mTipsContainer.animate().alpha(0.0f).setDuration(2000).start();
			return;
		}
	}
	
	/**
	 * Set or reset the timer.
	 */
	private  void setOrResetTimer () {

		// Check if you need to reset.
		if (mDisplayTimer != null) {
			Log.i(TAG, "[INFO] Reseting timer.");
			mDisplayTimer.cancel();
			mDisplayTimer = null;
			
		} else {
			Log.i(TAG, "[INFO] Init timer.");
		}
		
		// Start the Progress Bar.
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mProgressBar.setVisibility(View.VISIBLE);
		mProgressBar.startProgress(30000);

		// Set or Reset
		mDisplayTimer = new CountDownTimer(30000, 1000) {

			public void onTick(long millisUntilFinished) {

				// Validation that the preview is still going
				if (null == mCamera) {
					cancel();
					return;
				}

				// Display the correct guidance: 
				if (millisUntilFinished >= 21000 || 
						(millisUntilFinished >= 9000 && millisUntilFinished <= 10000 ) ) {
					displayBothTips(millisUntilFinished);
				}

				if (millisUntilFinished <= 2000) {
					if (mLiveCard.isPublished()) {
						mLiveCard.unpublish();
					}
				}
			}
			public void onFinish() {
				if (mCamera != null) {
					Log.i(TAG, "[INFO] Time's up!");
					mCamera.setPreviewCallback(null);
					mCamera.stopPreview();
					mCamera.release();
					mCamera = null;
					exitGlassware();
				}
				getBatteryLevel();
			}
		}.start();
	}

	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {

		// Press the camera button to reset the timer.
		if (keycode == KeyEvent.KEYCODE_CAMERA) {

			// reset the timer 
			setOrResetTimer();
			return true;
		}

		if (keycode == KeyEvent.KEYCODE_BACK) {
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
			Log.i(TAG, "[INFO] User tapped. Opening the Option menu.");
			
			//Play the tap sound.
			 AudioManager audio = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			 audio.playSoundEffect(Sounds.TAP);
			openOptionsMenu();
			return true;
		} else if (gesture == Gesture.TWO_TAP) {
			// do something on two finger tap
			Log.i(TAG, "[INFO] User tapped with two fingers.");
			return true;			
		} else 
			if (gesture == Gesture.TWO_SWIPE_RIGHT) {
			// do something on right (forward) swipe
			Log.i(TAG, "[INFO] User Swiped forward using two fingers.");
			zoomIn(10);
			return true;
		} else if (gesture == Gesture.TWO_SWIPE_LEFT) {
			// do something on left (backwards) swipe
			Log.i(TAG, "[INFO] User Swiped back using two fingers.");
			zoomOut(10);
			return true;
		}
		/* 
		 * dovik: At the request of the glass team, I commented all 1 finger swipes
		 * as they are not compliant with Gesture interaction on Glass.
		 */	
		//		} else if (gesture == Gesture.SWIPE_RIGHT) {
		//			// do something on right (forward) swipe
		//			Log.i(TAG, "[INFO] User Swiped forward.");
		//			zoomIn(10);
		//			return true;
		//		} else if (gesture == Gesture.SWIPE_LEFT) {
		//			// do something on left (backwards) swipe
		//			Log.i(TAG, "[INFO] User Swiped back.");
		//			zoomOut(10);
		//			return true;
		//		}	
		return false;
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

		Log.i(TAG, "[INFO] Battery Level after exit is: " +batteryLevel);
		return; 
	}

	/**
	 * Requests that the views redo their layout. This must be called manually every time the
	 * tips view's text is updated because this layout doesn't exist in a GUI thread where those
	 * requests will be enqueued automatically.
	 */
	private void doLayout() {
		// Measure and update the layout so that it will take up the entire surface space
		// when it is drawn.

		RelativeLayout layout = (RelativeLayout)findViewById(R.id.main_layout);

		int measuredWidth = MeasureSpec.makeMeasureSpec(layout.getWidth(), MeasureSpec.EXACTLY);
		int measuredHeight = MeasureSpec.makeMeasureSpec(layout.getHeight(), MeasureSpec.EXACTLY);

		layout.measure(measuredWidth, measuredHeight);
		layout.layout(0, 0, layout.getMeasuredWidth(), layout.getMeasuredHeight());
	}

	/**
	 * Show and hide and magnifying glass indicator. 
	 **/
	private void showZoomIndicator(int action) {

		int imageId = 0; 

		// select the correct image
		switch (action) {
		case ZOOM_IN:
			imageId = R.drawable.zoom_in_white;
			break;
		case ZOOM_OUT:
			imageId = R.drawable.zoom_out_white;
			break;
		case CANT_ZOOM_IN:
			imageId = R.drawable.cant_zoom_in_red;
			break;
		case CANT_ZOOM_OUT:
			imageId = R.drawable.cant_zoom_out_red;
			break;
		}

		if (null == mMagnifyingGlass) {
			mMagnifyingGlass = (ImageView)findViewById(R.id.magnifying_glass_indicator);
		}

		mMagnifyingGlass.setVisibility(View.VISIBLE);

		int timeBetween = 1000;
		int fadeOutDuration = 1000;

		mMagnifyingGlass.setImageResource(imageId);

		Animation fadeOut = new AlphaAnimation(1, 0);
		fadeOut.setStartOffset(timeBetween);
		fadeOut.setDuration(fadeOutDuration);
		fadeOut.setFillAfter(true);

		fadeOut.setAnimationListener(new Animation.AnimationListener(){
			@Override
			public void onAnimationStart(Animation arg0) {
			}           
			@Override
			public void onAnimationRepeat(Animation arg0) {
			}           
			@Override
			public void onAnimationEnd(Animation arg0) {
				mMagnifyingGlass.setImageResource(0);
			}
		});


		AnimationSet animation = new AnimationSet(true);
		animation.addAnimation(fadeOut);
		animation.setRepeatCount(1);
		mMagnifyingGlass.setAnimation(animation);
	}
	
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.zoomin_menu, menu);
		getLayoutInflater().setFactory(new Factory() {

			public View onCreateView(String name, Context context,
					AttributeSet attrs) {

				if (name.equalsIgnoreCase("TextView")) {
					try {
						LayoutInflater li = LayoutInflater.from(context);
						final View view = li.createView(name, null, attrs);
						new Handler().post(new Runnable() {
							public void run() {
								// set the text size.
								((TextView) view).setTextSize(40);
							}
						});
						return view;
					} catch (InflateException e) {
						Log.e(TAG, "[ERROR] an InflateException was thrown: " + e.getMessage());
					} catch (ClassNotFoundException e) {
						Log.e(TAG, "[ERROR] a ClassNotFoundException was thrown: " + e.getMessage());
					}
				}
				return null;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.op_relaunch:
			Log.i(TAG, "[INFO] Relaunching Magnify.");
			setOrResetTimer();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing to do here.
	}
}
