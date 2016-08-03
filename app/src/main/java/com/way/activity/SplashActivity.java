package com.way.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import com.way.consts.setting;
import com.way.util.PreferenceConstants;
import com.way.util.PreferenceUtils;
import com.way.xx.R;

import java.io.File;

public class SplashActivity extends FragmentActivity {
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		mHandler = new Handler();
		String password = PreferenceUtils.getPrefString(this,
				PreferenceConstants.PASSWORD, "");
		if (!TextUtils.isEmpty(password)) {
			mHandler.postDelayed(gotoMainAct, 3000);
		} else {
			mHandler.postDelayed(gotoLoginAct, 3000);
		}

		// 创建下载文件夹
        File destDir = new File(setting.savePath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
	}

	Runnable gotoLoginAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, LoginActivity.class));
			finish();
		}
	};

	Runnable gotoMainAct = new Runnable() {

		@Override
		public void run() {
			startActivity(new Intent(SplashActivity.this, MainActivity.class));
			finish();
		}
	};
}