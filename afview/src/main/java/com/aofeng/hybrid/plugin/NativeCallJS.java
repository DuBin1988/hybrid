/**
 * 本地调用h5中的js
 */

package com.aofeng.hybrid.plugin;

import android.util.Log;
import android.webkit.WebView;

import java.lang.ref.WeakReference;

public class NativeCallJS {
    private static final String CALLBACK_JS_FORMAT = "javascript:%s.callback(%d, %d %s);";
    private int mIndex;
    private boolean mCouldCallAgain;
    private WeakReference<WebView> mWebViewRef;
    private int mIsSticky;
    private String mInjectedName;

    public NativeCallJS (WebView view, String injectedName, int index) {
        mCouldCallAgain = true;
        mWebViewRef = new WeakReference<WebView>(view);
        mInjectedName = injectedName;
        mIndex = index;
    }

    public void apply (Object... args) throws JsCallbackException {
        if (mWebViewRef.get() == null) {
            throw new JsCallbackException("弱引用失效。");
        }
        if (!mCouldCallAgain) {
            throw new JsCallbackException("不能多次调用。");
        }
        StringBuilder sb = new StringBuilder();
        for (Object arg : args){
            sb.append(",");
            boolean isStrArg = arg instanceof String;
            if (isStrArg) {
                sb.append("\"");
            }
            sb.append(String.valueOf(arg));
            if (isStrArg) {
                sb.append("\"");
            }
        }
        String execJs = String.format(CALLBACK_JS_FORMAT, mInjectedName, mIndex, mIsSticky, sb.toString());
        Log.d("JsCallBack", execJs);
        mWebViewRef.get().loadUrl(execJs);
        mCouldCallAgain = mIsSticky > 0;
    }

    public void setPermanent (boolean value) {
        mIsSticky = value ? 1 : 0;
    }

    public static class JsCallbackException extends Exception {
		private static final long serialVersionUID = 1L;

		public JsCallbackException (String msg) {
            super(msg);
        }
    }
}
