/**
 * ChromeClient类，给页面注入调用native的js，对prompt进行拦截实现调用native代码
 */

package com.aofeng.hybrid.plugin;

import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aofeng.hybrid.util.LogUtil;

public class AFChromeClient extends WebChromeClient {
    private final String TAG = "AFChromeClient";
    private NativeStubParser mStub;
    private boolean mIsInjectedJS;
    
    private TextView mTvLog;
    private StringBuilder sb = new StringBuilder(512);


    public AFChromeClient (TextView logView, String injectedName, Class<?> injectedCls) {
    	this.mTvLog = logView;
        mStub = new NativeStubParser(injectedName, injectedCls);
    }

    public AFChromeClient (String injectedName, Class<?> injectedCls) {
    	this(null, injectedName, injectedCls);
    }
    
    
    /**
     * 页面来的alert全部不显示
     */
    @Override
    public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
        result.confirm();
        return true;
    }

    /**
     * 注入页面调用本地所需js
     */
    @Override
    public void onProgressChanged (WebView view, int newProgress) {
        //为什么要在这里注入JS
        //1 OnPageStarted中注入有可能全局注入不成功，导致页面脚本上所有接口任何时候都不可用
        //2 OnPageFinished中注入，虽然最后都会全局注入成功，但是完成时间有可能太晚，当页面在初始化调用接口函数时会等待时间过长
        //3 在进度变化时注入，刚好可以在上面两个问题中得到一个折中处理
        //为什么是进度大于25%才进行注入，因为从测试看来只有进度大于这个数字页面才真正得到框架刷新加载，保证100%注入成功
        if (newProgress <= 25) {
            mIsInjectedJS = false;
        } else if (!mIsInjectedJS) {
            view.loadUrl(mStub.getPreloadInterfaceJS());
            mIsInjectedJS = true;
            Log.d(TAG, "注入js调用到页面在加载到：" + newProgress);

        }
        //显示加载进度
        if (view instanceof AFWebView) {
        	AFWebView pwv = (AFWebView) view;
        	ProgressBar pb = pwv.getmProgressBar();
            pb.setProgress(newProgress);
            if (newProgress > 90) {
                pb.setVisibility(View.GONE);
            } else {
                pb.setVisibility(View.VISIBLE);
            }
        }

        super.onProgressChanged(view, newProgress);
    }

    /**
     * 拦截页面来的prompt 
     * intercept prompt calls from h5 page
     * and delegate them to native
     * json: {method:'', types:[], arguments:[]}
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String json, String defaultValue, JsPromptResult result) {
    	//对prompt做出响应
        result.confirm(mStub.call(view, json));
        return true;
    }

	@Override
	public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (mTvLog != null) {
            if (sb.length() > 512) {
                String tmp = sb.substring(256);
                sb.setLength(0);
                sb.append(tmp);
            }
            sb.append(consoleMessage.message());
            sb.append("\n");
            mTvLog.setText(sb);
        }
        LogUtil.d("onConsoleMessage", consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
	}
    
    
}
