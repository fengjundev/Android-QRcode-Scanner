package com.feng.qrcode.toolbox;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * The screen density expressed as dots-per-inch tools
 * 
 * @author fengjun
 */
public class DensityUtils {
	
	private float mDensityDpi = 0.0f;
	private DisplayMetrics mDisplayMetrics;
	
	/**
	 * scale rate to 160
	 */
	private float scale = 0.0f;

	public DensityUtils(Context context) {
		mDisplayMetrics = context.getApplicationContext().getResources().getDisplayMetrics();
		mDensityDpi = mDisplayMetrics.densityDpi;
		scale = mDensityDpi / DisplayMetrics.DENSITY_DEFAULT;
	}

	public int getScreenWidth() {
		return mDisplayMetrics.widthPixels;
	}

	public int getScreenHeight() {
		return mDisplayMetrics.heightPixels;
	}

	public float getDensityDpi() {
		return mDensityDpi;
	}

	public int dip2px(float dipValue) {
		return (int) (dipValue * scale + 0.5f);
	}

	public int px2dip(float pxValue) {
		return (int) (pxValue / scale + 0.5f);
	}

	@Override
	public String toString() {
		return "DensityDpi:" + mDensityDpi;
	}
}
