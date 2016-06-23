/**
 * WebViewClient 负责处理浏览器级别事件
 */

package com.aofeng.hybrid.plugin;

import android.app.Activity;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.aofeng.hybrid.util.LogUtil;

public class AFWebViewClient extends WebViewClient {
    private static final String TAG = "AFWebViewClient";
    
    private Activity mActivity;
    
    public AFWebViewClient(Activity activity)
    {
    	this.mActivity = activity;
    }
    
	/**
	 * called when a new page is about to open
	 * 强制在嵌入浏览器打开
	 */
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		view.loadUrl(url);
		return false;
	}

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        LogUtil.e(TAG, "onReceivedError = " + failingUrl);
        LogUtil.e(TAG, "errorCode = " + errorCode + " description " + description);
        super.onReceivedError(view, errorCode, description, failingUrl);
    }
	
    /**
     * on finish loading the h5 page
     */
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (mActivity != null) {
            mActivity.setTitle(view.getTitle());
        }
    }
    
	/**
	 * 对于某些页面不保留回退历史
	 * can modify the response when n url is intercepted
	 */
	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view,	String url) {
		//拦截http请求，如果是离线，则从本地提取数据
		//append user random data for security purpose
		//cache static resource 
		//this can be used to fabricate
		return super.shouldInterceptRequest(view, url);
	}
}
