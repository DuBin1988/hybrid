package com.aofeng.hybrid.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import com.aofeng.hybrid.activity.HybridActivity;
import com.aofeng.hybrid.plugin.H5Param;
import com.aofeng.hybrid.util.LogUtil;

import org.json.JSONObject;

import java.io.File;

/**
 * 本地方法基础类
 * @author LGY
 *
 */
public class NativeBaseMethod {
    /**
     * 打开h5页面或者是本地页面g
     * @param webView 浏览器
     * @param jo 页json对象
     * */
    @SuppressLint("DefaultLocale")
	public static void _open_a_page (WebView webView, JSONObject jo) {
    	try {
        	Context context = webView.getContext();
	        if(jo.getString("type").toLowerCase().equals("native")) {
	    		startupNativePage(jo.getString("page"), jo.getString("param"), context);
	        } else {
	    		startupH5Page(jo.getString("page"), jo.getString("method"), context);
	        }
    	} catch(Exception e) {
    		LogUtil.d("NativeBaseMethod", "打开新页面出错。");
    	}
    }

	private static void startupNativePage(String page, String param, Context context)
			throws ClassNotFoundException {
        //page com.aofeng.hybrid.activity.xxxActivity
        LogUtil.d("NativeBaseMethod", page);
        Intent intent = new Intent(context, Class.forName(page));
		Bundle bundle = new Bundle();
		bundle.putString("param", param);
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

	private static void startupH5Page(String page, String method, Context context) {
		Intent intent = new Intent(context, HybridActivity.class);
		Bundle bundle = new Bundle();
		if(!page.startsWith("http://"))
			page = "file://" + context.getFilesDir().getAbsolutePath()+ File.separator + "www/" + page;
        LogUtil.d("NativeBaseMethod", page);
        //method: com.aofeng.hybrid.android.Page1Method
		bundle.putSerializable("param", new H5Param(page, method));
		intent.putExtras(bundle);
		context.startActivity(intent);
	}

}