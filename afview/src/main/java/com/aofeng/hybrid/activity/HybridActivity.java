package com.aofeng.hybrid.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;

import com.aofeng.hybrid.R;
import com.aofeng.hybrid.plugin.AFChromeClient;
import com.aofeng.hybrid.plugin.AFWebViewClient;
import com.aofeng.hybrid.plugin.H5Param;
import com.aofeng.hybrid.util.LogUtil;

public class HybridActivity extends Activity {

	/** CONSTANTS **/

	private static final String TAG = HybridActivity.class.getSimpleName();

	private WebView mWebView;

	@SuppressLint("SetJavaScriptEnabled")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		H5Param hp = (H5Param)bundle.getSerializable("param");
		setContentView(R.layout.main);
		mWebView = (WebView) findViewById(R.id.wvPortal);
		//注入的bridge名称为HostApp
		mWebView.setWebViewClient(new AFWebViewClient(this));
		try {
			mWebView.setWebChromeClient(new AFChromeClient("HostApp", Class.forName(hp.clazz)));
		} catch (ClassNotFoundException e) {
			LogUtil.d(TAG, "打开h5页面错误。" + hp.clazz);
		}
		WebSettings mWebSettings = mWebView.getSettings();
		mWebSettings.setJavaScriptEnabled(true);
		mWebSettings.setAllowFileAccess(true);
		mWebSettings.setDomStorageEnabled(true);
		mWebSettings.setRenderPriority(RenderPriority.HIGH);
		mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		mWebView.requestFocus(View.FOCUS_DOWN);
		
		if (savedInstanceState != null) {
			mWebView.restoreState(savedInstanceState);
		} else {
			//android assets: file:///android_asset/test.html
			mWebView.loadUrl(hp.page);
		}	
		LogUtil.d(TAG, hp.page);
	}

	@Override
	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			mWebView.goBack();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mWebView.saveState(outState);
	}

}
