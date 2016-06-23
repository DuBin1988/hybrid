package com.aofeng.hybrid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.File;

public class CommUtil {

	/**
	 * 判断是否联网
	 */
	public static boolean hasNetwork(Context context)
	{
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] nwis = cm.getAllNetworkInfo();
		for(NetworkInfo nwi : nwis)
		{
			if(nwi.getState() == NetworkInfo.State.CONNECTED)
			{
				if(nwi.getType() == ConnectivityManager.TYPE_WIFI)
					return true;
				if(nwi.getType() == ConnectivityManager.TYPE_MOBILE)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * 存储偏好
	 */
	public static void savePreference(Context context, String appId, String key, String value)
	{
		SharedPreferences.Editor prefEditor = context.getSharedPreferences(appId, 1).edit();
		prefEditor.putString(key, value);
		prefEditor.commit();
	}
	
	/**
	 * 获取偏好，不存在返回null
	 */
	public static String getPreference(Context context, String appId, String key)
	{
		return context.getSharedPreferences(appId, 1).getString(key, null);
	}
	
	/**
	 * 获取版本号
	 * @param context
	 * @return
	 */
	public static int getVersionCode(Context context, String packageName) {
		try
		{
		    PackageInfo manager= context.getPackageManager().getPackageInfo(packageName,0);
		    return manager.versionCode;
		}
		catch(Exception e)
		{
			return -1;
		}
	}

	/**
	 * 设置连接诶属性，建立连接超时5秒，等待数据超时30秒
	 * @return
	 */
	public static HttpClient getTimeoutHttpClient()
	{
		HttpParams httpParameters = new BasicHttpParams();
		// Set the timeout in milliseconds until a connection is established.
		// The default value is zero, that means the timeout is not used. 
		int timeoutConnection = 5000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
		// Set the default socket timeout (SO_TIMEOUT) 
		// in milliseconds which is the timeout for waiting for data.
		int timeoutSocket = 30000;
		HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
		return new DefaultHttpClient(httpParameters);
	}

	
    public static void evaluateJavascript(WebView mWebview, String script, ValueCallback<String> resultCallback) {
    	mWebview.loadUrl("javascript:" + script);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebview.evaluateJavascript(script, resultCallback);
        } else {
            mWebview.loadUrl("javascript:" + script);
        }
    }

	/**
	 * 获取文件路径
	 * @param context
	 * @return
     */
	public static String getFilePath(Context context) {
		return context.getFilesDir().getAbsolutePath()+ File.separator;
	}
 }
