package com.google.glass.glassware.zoomin;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * This class represents a Camera Preview.
 * 
 * @author dovik
 *
 */
public class GlassCameraPreview extends SurfaceView implements SurfaceHolder.Callback {

	private SurfaceHolder 			mHolder;
	public  Camera 					mCamera;
	private String 					TAG = "ZoomIn";
	
	@SuppressWarnings("deprecation")
	public GlassCameraPreview(Context context, Camera camera) {
		super(context);
		
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}
	
	public GlassCameraPreview(Context context) {
		super(context);
		Log.e(TAG, "[ERROR] invalid camera preview.");
	
	}

	public void surfaceCreated(SurfaceHolder holder) {
		
		if (mCamera == null) {
			PerformZoomActivity.getCameraInstance();
		}

		// The Surface has been created, now tell the camera where to draw the preview.
		try {
			mCamera.setPreviewDisplay(holder);

			setCameraParameters();

			mCamera.startPreview();
		} catch (IOException e) {
			Log.e(TAG, "[ERROR] Error setting camera preview: " + e.getMessage());
		}
	}
	
	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (mHolder.getSurface() == null) {
			return;
		}
		
		// Stop the preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// Do nothing.
		}
		try {
			// Make changes.
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.set("preview-frame-rate", "30");
			parameters.set("preview-fps-range", "30000,30000");
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e) {
			Log.i(TAG, "[ERROR] Failed to create camera preview " + e.toString());
		}
	}
	
	
	/**
	 * Set Camera Parameters.
	 **/
	private void setCameraParameters () {

		if (mCamera == null) {
			Log.e(TAG, "[ERROR] The Camera is invalid. Exiting.");
			return;
		}

		Camera.Parameters params = mCamera.getParameters();
		
		// Print all the camera's parametes for debugging purposes.
		//Log.i(TAG, "[DEBUG] CAMERA PARAMS: " + params.flatten());

		params.set("preview-frame-rate", "30");
		params.set("preview-fps-range", "30000,30000");
		params.set("preview-size", "640x360");
		mCamera.setParameters(params);
	}
	
	public SurfaceHolder getSurfaceHolder() {
		return mHolder;
	}
}
