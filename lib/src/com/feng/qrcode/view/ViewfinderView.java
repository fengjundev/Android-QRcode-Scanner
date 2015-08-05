/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.feng.qrcode.view;

import java.util.ArrayList;
import java.util.List;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.feng.qrcode.R;
import com.feng.qrcode.camera.CameraManager;
import com.feng.qrcode.toolbox.DensityUtils;
import com.google.zxing.ResultPoint;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

	private static final int CURRENT_POINT_OPACITY = 0xA0;
	private static final int MAX_RESULT_POINTS = 20;
	private static final int POINT_SIZE = 6;
	private static final int SLIDER_PADDING = 5;
	private static final int SLIDER_ANIM_DURATION = 3000;

	private CameraManager cameraManager;
	private final Paint paint;
	private Bitmap resultBitmap;
	private Bitmap sliderBitmap;
	
	private final int maskColor;
	private final int resultColor;
	private final int resultPointColor;
	private final int middleFrameColor;
	
	private int sliderTop;
	
	private List<ResultPoint> possibleResultPoints;
	private List<ResultPoint> lastPossibleResultPoints;
	private ValueAnimator sliderAnimator;
	
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		middleFrameColor = resources.getColor(R.color.middle_frame_color);
		possibleResultPoints = new ArrayList<ResultPoint>(5);
		lastPossibleResultPoints = null;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cancelAnimator();
	}
	
	public void destroy(){
		cancelAnimator();
	}

	/**
	 * Remove scan laser animator when Activity destroy or window is hide
	 */
	private void cancelAnimator() {
		if(sliderAnimator != null && sliderAnimator.isRunning()){
			sliderAnimator.cancel();
			sliderAnimator = null;
		}
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		if (cameraManager == null) {
			return; // not ready yet, early draw before done configuring
		}
		
		Rect frame = cameraManager.getFramingRect();
		
		if (frame == null) {
			return;
		}
		
		drawMask(canvas, frame);

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(CURRENT_POINT_OPACITY);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {
			drawMiddleFrame(canvas, frame);
			drawScannerLine(canvas, frame);
			drawPossiblePoints(canvas, frame);
		}
	}

	private void drawMiddleFrame(Canvas canvas, Rect frame) {
		paint.setColor(middleFrameColor);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(2);
		canvas.drawRect(frame, paint);
		paint.setStyle(Style.FILL);
	}

	/**
	 * Draw a moving scanner line to show QrCode is being encoding
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawScannerLine(Canvas canvas, final Rect frame) {
		if(sliderBitmap == null || sliderBitmap.isRecycled()){
			sliderBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.scan_slider_line);
		}
		
		Rect lineRect = new Rect();
		lineRect.left = frame.left + SLIDER_PADDING;
		lineRect.right = frame.right - SLIDER_PADDING;
		lineRect.top = sliderTop;
		lineRect.bottom = (sliderTop + sliderBitmap.getHeight());
		
		if(sliderAnimator == null){
			sliderTop = frame.top;
			sliderAnimator = ValueAnimator.ofInt(frame.top, frame.bottom - lineRect.height());
			sliderAnimator.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					sliderTop = (Integer) animation.getAnimatedValue();
					postInvalidateDelayed(0, 
							frame.left - POINT_SIZE,
							frame.top - POINT_SIZE, 
							frame.right + POINT_SIZE,
							frame.bottom + POINT_SIZE);
				}
			});
			sliderAnimator.setRepeatCount(ValueAnimator.INFINITE);
			sliderAnimator.setRepeatMode(ValueAnimator.REVERSE);
			sliderAnimator.setDuration(SLIDER_ANIM_DURATION);
			sliderAnimator.start();
		}

		canvas.drawBitmap(sliderBitmap, null, lineRect, paint);
	}

	private void drawPossiblePoints(Canvas canvas, Rect frame) {
		Rect previewFrame = cameraManager.getFramingRectInPreview();
		if (previewFrame == null) {
			return;
		}
		
		float scaleX = frame.width() / (float) previewFrame.width();
		float scaleY = frame.height() / (float) previewFrame.height();

		List<ResultPoint> currentPossible = possibleResultPoints;
		List<ResultPoint> currentLast = lastPossibleResultPoints;
		int frameLeft = frame.left;
		int frameTop = frame.top;
		if (currentPossible.isEmpty()) {
			lastPossibleResultPoints = null;
		} else {
			possibleResultPoints = new ArrayList<ResultPoint>(5);
			lastPossibleResultPoints = currentPossible;
			paint.setAlpha(CURRENT_POINT_OPACITY);
			paint.setColor(resultPointColor);
			synchronized (currentPossible) {
				for (ResultPoint point : currentPossible) {
					canvas.drawCircle(frameLeft
							+ (int) (point.getX() * scaleX), frameTop
							+ (int) (point.getY() * scaleY), POINT_SIZE,
							paint);
				}
			}
		}
		if (currentLast != null) {
			paint.setAlpha(CURRENT_POINT_OPACITY / 2);
			paint.setColor(resultPointColor);
			synchronized (currentLast) {
				float radius = POINT_SIZE / 2.0f;
				for (ResultPoint point : currentLast) {
					canvas.drawCircle(frameLeft
							+ (int) (point.getX() * scaleX), frameTop
							+ (int) (point.getY() * scaleY), radius, paint);
				}
			}
		}
	}

	/**
	 * Draw dark cover outside the middle frame
	 * 
	 * @param canvas
	 * @param frame
	 */
	private void drawMask(Canvas canvas, Rect frame) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(resultBitmap != null ? resultColor : maskColor);

		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);
	}

	public void drawViewfinder() {
		Bitmap resultBitmap = this.resultBitmap;
		this.resultBitmap = null;
		if (resultBitmap != null) {
			resultBitmap.recycle();
		}
		invalidate();
	}

	public void setCameraManager(CameraManager cameraManager) {
		this.cameraManager = cameraManager;
	}
	
	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 *
	 * @param barcode An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		List<ResultPoint> points = possibleResultPoints;
		synchronized (points) {
			points.add(point);
			int size = points.size();
			if (size > MAX_RESULT_POINTS) {
				// trim it
				points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
			}
		}
	}

}
