package cn.feng.qrcode.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import cn.feng.qrcode_demo.R;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.QRcodeIntents;
import com.google.zxing.client.android.toolbox.QrcodeUtils;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final EditText ed = (EditText) findViewById(R.id.editText1);
		ed.setText("https://www.google.com/");
		findViewById(R.id.scan_btn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, CaptureActivity.class);  
                startActivityForResult(intent, CaptureActivity.SCAN_QRCODE_REQUEST_CODE);
			}
		});
		
		findViewById(R.id.create_qrcode).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = ed.getText().toString().trim();
				QrcodeUtils.launchCreateQrcodeActivity(text, MainActivity.this);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(resultCode == RESULT_OK && requestCode == CaptureActivity.SCAN_QRCODE_REQUEST_CODE){
			String resultStr = data.getStringExtra(QRcodeIntents.Scan.RESULT_SCAN_QRCODE_TEXT);
			Toast.makeText(getApplicationContext(), "扫描成功", Toast.LENGTH_LONG).show();
			TextView mTextView = (TextView)findViewById(R.id.scan_result);
			mTextView.setText(resultStr);
		}
	}
}
