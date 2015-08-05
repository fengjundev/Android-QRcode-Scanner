package com.feng.qrcode.toolbox;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.feng.qrcode.Contents;
import com.feng.qrcode.QRcodeIntents;
import com.google.zxing.BarcodeFormat;

public class QrcodeUtils {
	
    private QrcodeUtils() {
        throw new AssertionError();
    }
    
    public static void launchCreateQrcodeActivity(String text, Context context){
    	if (TextUtils.isEmpty(text)) {
    	   return;
    	}
        Intent intent = new Intent(QRcodeIntents.Encode.ACTION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(QRcodeIntents.Encode.TYPE, Contents.Type.TEXT);
        intent.putExtra(QRcodeIntents.Encode.DATA, text);
        intent.putExtra(QRcodeIntents.Encode.FORMAT, BarcodeFormat.QR_CODE.toString());
        context.startActivity(intent);
    }
}
