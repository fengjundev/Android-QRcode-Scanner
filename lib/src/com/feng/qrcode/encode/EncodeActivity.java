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

package com.feng.qrcode.encode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.feng.qrcode.FinishListener;
import com.feng.qrcode.QRcodeIntents;
import com.feng.qrcode.R;
import com.google.zxing.WriterException;

/**
 * This class encodes data from an Intent into a QR code, and then displays it full screen so that
 * another person can scan it with their device.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class EncodeActivity extends Activity {

  private static final String TAG = EncodeActivity.class.getSimpleName();

  private static final int MAX_BARCODE_FILENAME_LENGTH = 24;
  private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");
  private static final String USE_VCARD_KEY = "USE_VCARD";

  private QRCodeEncoder qrCodeEncoder;

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    Intent intent = getIntent();
    if (intent == null) {
      finish();
    } else {
      String action = intent.getAction();
      if (QRcodeIntents.Encode.ACTION.equals(action) || Intent.ACTION_SEND.equals(action)) {
        setContentView(R.layout.activity_encode);
      } else {
        finish();
      }
    }
  }

  public void saveAndShare() {
    QRCodeEncoder encoder = qrCodeEncoder;
    if (encoder == null) { 
      Log.w(TAG, "No existing barcode to send?");
      return;
    }

    String contents = encoder.getContents();
    if (contents == null) {
      Log.w(TAG, "No existing barcode to send?");
      return;
    }

    Bitmap bitmap;
    try {
      Bitmap waterMask = BitmapFactory.decodeResource(getResources(), R.drawable.app_water_mask);  
      bitmap = encoder.encodeAsBitmap(waterMask);
    } catch (WriterException we) {
      Log.w(TAG, we);
      return;
    }
    if (bitmap == null) {
      return;
    }

    File bsRoot = new File(Environment.getExternalStorageDirectory(), "BarcodeScanner");
    File barcodesRoot = new File(bsRoot, "Barcodes");
    if (!barcodesRoot.exists() && !barcodesRoot.mkdirs()) {
      Log.w(TAG, "Couldn't make dir " + barcodesRoot);
      showErrorMessage(R.string.msg_unmount_usb);
      return;
    }
    File barcodeFile = new File(barcodesRoot, makeBarcodeFileName(contents) + ".png");
    if (!barcodeFile.delete()) {
      Log.w(TAG, "Could not delete " + barcodeFile);
      // continue anyway
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(barcodeFile);
      bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
    } catch (FileNotFoundException fnfe) {
      Log.w(TAG, "Couldn't access file " + barcodeFile + " due to " + fnfe);
      showErrorMessage(R.string.msg_unmount_usb);
      return;
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ioe) {
          // do nothing
        }
      }
    }

    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + encoder.getTitle());
    intent.putExtra(Intent.EXTRA_TEXT, contents);
    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + barcodeFile.getAbsolutePath()));
    intent.setType("image/png");
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    startActivity(Intent.createChooser(intent, null));
  }

  private static CharSequence makeBarcodeFileName(CharSequence contents) {
    String fileName = NOT_ALPHANUMERIC.matcher(contents).replaceAll("_");
    if (fileName.length() > MAX_BARCODE_FILENAME_LENGTH) {
      fileName = fileName.substring(0, MAX_BARCODE_FILENAME_LENGTH);
    }
    return fileName;
  }

  @Override
  protected void onResume() {
    super.onResume();
    // This assumes the view is full screen, which is a good assumption
    WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
    Display display = manager.getDefaultDisplay();
    Point displaySize = new Point();
    display.getSize(displaySize);
    int width = displaySize.x;
    int height = displaySize.y;
    int smallerDimension = width < height ? width : height;
    smallerDimension = smallerDimension * 7 / 8;

    Intent intent = getIntent();
    if (intent == null) {
      return;
    }

    try {
      boolean useVCard = intent.getBooleanExtra(USE_VCARD_KEY, false);
      qrCodeEncoder = new QRCodeEncoder(this, intent, smallerDimension, useVCard);
      
      Bitmap waterMask = BitmapFactory.decodeResource(getResources(), R.drawable.app_water_mask);  
      Bitmap bitmap = qrCodeEncoder.encodeAsBitmap(waterMask);
      if (bitmap == null) {
        Log.w(TAG, "Could not encode barcode");
        showErrorMessage(R.string.msg_encode_contents_failed);
        qrCodeEncoder = null;
        return;
      }

      ImageView view = (ImageView) findViewById(R.id.image_view);
      view.setImageBitmap(bitmap);

      if (intent.getBooleanExtra(QRcodeIntents.Encode.SHOW_CONTENTS, true)) {
        setTitle(qrCodeEncoder.getTitle());
      } else {
        setTitle("");
      }
    } catch (WriterException e) {
      Log.w(TAG, "Could not encode barcode", e);
      showErrorMessage(R.string.msg_encode_contents_failed);
      qrCodeEncoder = null;
    }
  }

  private void showErrorMessage(int message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(message);
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }
}
