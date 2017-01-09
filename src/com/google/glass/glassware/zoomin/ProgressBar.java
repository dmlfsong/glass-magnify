package com.google.glass.glassware.zoomin;

import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;


/**
 * A simple progress bar in glass style.
 */
public class ProgressBar extends ImageView {

	/**
	 * The max level that can be set via {@link android.graphics.drawable.Drawable#setLevel(int)}.
	 * */
	private static final int DRAWABLE_MAX_LEVEL = 10000;

	/**
	 *  {@code true} if the progress bar is indeterminate, {@code false} otherwise.
	 **/
	@SuppressWarnings("unused")
	private boolean indeterminate;
	private int progress = 0;
	private int maxProgress = DRAWABLE_MAX_LEVEL;
	private boolean isShowing = true;
	private ObjectAnimator progressAnimator;

	public ProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		setIndeterminate(false);
	}

	/**
	 *  Sets if this {@link ProgressBar} is indeterminate (aka, unknown total progress length. 
	 **/
	public void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
		Resources res = getResources();
		if (indeterminate) {
			setImageDrawable(res.getDrawable(R.drawable.slider_intermediate));
		} else {
			setImageDrawable(getResources().getDrawable(R.drawable.progress_bar_determinate));
		}
	}

	/**
	 * Shows this {@link ProgressBar}.
	 **/
	public void show(boolean animate) {
		if (isShowing) {
			return;
		}

		if (animate) {
			animate().translationY(0).setDuration(
					getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));
		} else {
			setTranslationY(0);
		}
		isShowing = true;
	}

	/**
	 *  Hides the {@link ProgressBar}. 
	 **/
	public void hide(boolean animate) {
		if (!isShowing) {
			return;
		}

		int height = getResources().getDimensionPixelSize(R.dimen.slider_bar_height);

		if (animate) {
			animate().translationY(height).setDuration(
					getResources().getInteger(R.integer.slider_in_out_animation_duration_ms));
		} else {
			setTranslationY(height);
		}
		isShowing = false;
	}

	/**
	 *  Sets the max value for this {@link ProgressBar}. 
	 **/
	public void setMax(int max) {
		this.maxProgress = max;
	}

	/**
	 * Calculates the level to set via {@link android.graphics.drawable.Drawable#setLevel(int)} using
	 * the current {@link #progress} and {@link #maxProgress}.
	 */
	private int calculateDrawableLevel() {
		return DRAWABLE_MAX_LEVEL * progress / maxProgress;
	}

	/**
	 * Sets the current progress for this {@link ProgressBar}.
	 */
	public void setProgress(int progress) {
		this.progress = Math.max(Math.min(progress, maxProgress), 0);
		getDrawable().setLevel(calculateDrawableLevel());
	}

	/**
	 *  Shows a continuously animating indeterminate progress bar.
	 **/
	public void startIndeterminate() {
		//	Assert.assertTrue(indeterminate);
		((AnimationDrawable) getDrawable()).start();
	}

	/**
	 *  Hides the indeterminate progress bar.
	 **/
	public void stopIndeterminate() {
		((AnimationDrawable) getDrawable()).stop();
	}

	/**
	 * Animates a progress bar from left to right using a {@link AccelerateDecelerateInterpolator}
	 * suitable for short animations. For longer/timed animations, please use
	 * {@link #startProgress(long, TimeInterpolator)}.
	 **/
	public void startProgress(long animationDuration) {
		startProgress(animationDuration, new AccelerateDecelerateInterpolator());
	}

	/** 
	 * {@link #startProgress(long)} with an {@link AnimatorListener}. 
	 **/
	public void startProgress(long animationDuration, AnimatorListener listener) {
		startProgress(animationDuration, new AccelerateDecelerateInterpolator(), listener);
	}

	/**
	 * Animates a progress bar from left to right using a TimeInterpolator of your choice.
	 **/
	public void startProgress(long animationDuration, TimeInterpolator interpolator) {
		startProgress(animationDuration, interpolator, null);
	}

	/**
	 *  {@link #startProgress(long, TimeInterpolator) with an {@link AnimatorListener}. 
	 **/
	public void startProgress(long animationDuration, TimeInterpolator interpolator,
			AnimatorListener animatorListener) {
		setIndeterminate(false);
		show(false);

		progressAnimator = ObjectAnimator.ofInt(this, "progress", 0, DRAWABLE_MAX_LEVEL);
		progressAnimator.setDuration(animationDuration);
		progressAnimator.setInterpolator(interpolator);
		if (animatorListener != null) {
			progressAnimator.addListener(animatorListener);
		}
		progressAnimator.start();
	}

	/**
	 * Cancels the progress of a slider started with {@link #startProgress(long, TimeInterpolator),
	 * animating it out to hidden.
	 */
	public void stopProgress() {
		if (progressAnimator != null) {
			progressAnimator.cancel();
		}
		hide(true);
	}
}