package com.aofeng.hybrid.plugin;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class NativeStubParser {
    private final static String TAG = "NativeStubParser";
    private final static String RETURN_RESULT_FORMAT = "{\"code\": %d, \"result\": %s}";
    private HashMap<String, Method> mMethodsMap;
    private String mBridgeName;
    private String mInjectedJs;
    private Gson mGson;

    /**动态生成的注入js
		(function(global){
		    console.log("HostApp initialization begin");
		    var hostApp = {
		        queue: [],
		        callback: function () {
		            var args = Array.prototype.slice.call(arguments, 0);
		            var index = args.shift();
		            var isPermanent = args.shift();
		            this.queue[index].apply(this, args);
		            if (!isPermanent) {
		                delete this.queue[index];
		            }
		        }
		    };
		    
		    //函数统一入口
		    hostApp.toast = hostApp.alert = hostApp.getIMSI = function () {
		    	//make arguments a genuine array
		        var args = Array.prototype.slice.call(arguments, 0);
		        if (args.length < 1) {
		            throw "HostApp call error, message:miss method name";
		        }
		        var aTypes = [];
		        for (var i = 1;i < args.length;i++) {
		            var arg = args[i];
		            var type = typeof arg;
		            aTypes[aTypes.length] = type;
		            if (type == "function") {
		                var index = hostApp.queue.length;
		                hostApp.queue[index] = arg;
		                args[i] = index;
		            }
		        }
		        var res = JSON.parse(prompt(JSON.stringify({
		            method: args.shift(),
		            types: aTypes,
		            args: args
		        })));
		
		        if (res.code != 200) {
		            throw "HostApp call error, code:" + res.code + ", message:" + res.result;
		        }
		        return res.result;
		    };
		

		    Object.getOwnPropertyNames(hostApp).forEach(function (property) {
		        var original = hostApp[property];
		
		        if (typeof original === 'function'&&property!=="callback") {
		            hostApp[property] = function () {
		                return original.apply(hostApp,  [property].concat(Array.prototype.slice.call(arguments, 0)));
		            };
		        }
		    });
		    global.HostApp = hostApp;
		    console.log("HostApp initialization end");
		})(window); 
     */
    
    /**
     * 生成从页面要调用的js函数 存根
     * @param bridgeName
     * @param injectedCls
     */
    public NativeStubParser (String bridgeName, Class<?> injectedCls) {
        try {
            if (TextUtils.isEmpty(bridgeName)) {
                throw new Exception("注入的JS类名不能为空。");
            }
            mBridgeName = bridgeName;
            mMethodsMap = new HashMap<String, Method>();
            StringBuilder sb = new StringBuilder("javascript:(function(b){console.log(\"");
            sb.append(mBridgeName);
            sb.append(" initialization begin\");var a={queue:[],callback:function(){var d=Array.prototype.slice.call(arguments,0);var c=d.shift();var e=d.shift();this.queue[c].apply(this,d);if(!e){delete this.queue[c]}}};");
            while(injectedCls != null) {
	            Method[] methods = injectedCls.getDeclaredMethods();
	            for (Method method : methods) {
	                String sign;
	                if (method.getModifiers() != (Modifier.PUBLIC | Modifier.STATIC) || (sign = genJavaMethodSign(method)) == null) {
	                    continue;
	                }
	                mMethodsMap.put(sign, method);
	                sb.append(String.format("a.%s=", method.getName()));
	            }
	            injectedCls = injectedCls.getSuperclass();
            }
            sb.append("function(){var f=Array.prototype.slice.call(arguments,0);if(f.length<1){throw\"");
            sb.append(mBridgeName);
            sb.append(" call error, message:miss method name\"}var e=[];for(var h=1;h<f.length;h++){var c=f[h];var j=typeof c;e[e.length]=j;if(j==\"function\"){var d=a.queue.length;a.queue[d]=c;f[h]=d}}var g=JSON.parse(prompt(JSON.stringify({method:f.shift(),types:e,args:f})));if(g.code!=200){throw\"");
            sb.append(mBridgeName);
            sb.append(" call error, code:\"+g.code+\", message:\"+g.result}return g.result};Object.getOwnPropertyNames(a).forEach(function(d){var c=a[d];if(typeof c===\"function\"&&d!==\"callback\"){a[d]=function(){return c.apply(a,[d].concat(Array.prototype.slice.call(arguments,0)))}}});b.");
            sb.append(mBridgeName);
            sb.append("=a;console.log(\"");
            sb.append(mBridgeName);
            sb.append(" initialization end\")})(window);");
            mInjectedJs = sb.toString();
        } catch(Exception e){
            Log.e(TAG, "init js error:" + e.getMessage());
        }
    }

    private String genJavaMethodSign (Method method) {
        String sign = method.getName();
        Class<?>[] argsTypes = method.getParameterTypes();
        int len = argsTypes.length;
        if (len < 1 || argsTypes[0] != WebView.class) {
            Log.w(TAG, "方法(" + sign + ") 第一个参数必须是 WebView");
            return null;
        }
        for (int k = 1; k < len; k++) {
            Class<?> cls = argsTypes[k];
            if (cls == String.class) {
                sign += "_S";
            } else if (cls == int.class ||
                cls == long.class ||
                cls == float.class ||
                cls == double.class) {
                sign += "_N";
            } else if (cls == boolean.class) {
                sign += "_B";
            } else if (cls == JSONObject.class) {
                sign += "_O";
            } else if (cls == NativeCallJS.class) {
                sign += "_F";
            } else {
                sign += "_P";
            }
        }
        return sign;
    }

    public String getPreloadInterfaceJS () {
        return mInjectedJs;
    }

    public String call(WebView webView, String jsonStr) {
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONObject callJson = new JSONObject(jsonStr);
                String methodName = callJson.getString("method");
                JSONArray argsTypes = callJson.getJSONArray("types");
                JSONArray argsVals = callJson.getJSONArray("args");
                String sign = methodName;
                int len = argsTypes.length();
                Object[] values = new Object[len + 1];
                int numIndex = 0;
                String currType;

                values[0] = webView;

                for (int k = 0; k < len; k++) {
                    currType = argsTypes.optString(k);
                    if ("string".equals(currType)) {
                        sign += "_S";
                        values[k + 1] = argsVals.isNull(k) ? null : argsVals.getString(k);
                    } else if ("number".equals(currType)) {
                        sign += "_N";
                        numIndex = numIndex * 10 + k + 1;
                    } else if ("boolean".equals(currType)) {
                        sign += "_B";
                        values[k + 1] = argsVals.getBoolean(k);
                    } else if ("object".equals(currType)) {
                        sign += "_O";
                        values[k + 1] = argsVals.isNull(k) ? null : argsVals.getJSONObject(k);
                    } else if ("function".equals(currType)) {
                        sign += "_F";
                        values[k + 1] = new NativeCallJS(webView, mBridgeName, argsVals.getInt(k));
                    } else {
                        sign += "_P";
                    }
                }

                Method currMethod = mMethodsMap.get(sign);

                // 方法匹配失败
                if (currMethod == null) {
                    return getJsonResult(jsonStr, 500, "not found method(" + sign + ") with valid parameters");
                }
                // 数字类型细分匹配
                if (numIndex > 0) {
                    Class<?>[] methodTypes = currMethod.getParameterTypes();
                    int currIndex;
                    Class<?> currCls;
                    while (numIndex > 0) {
                        currIndex = numIndex - numIndex / 10 * 10;
                        currCls = methodTypes[currIndex];
                        if (currCls == int.class) {
                            values[currIndex] = argsVals.getInt(currIndex - 1);
                        } else if (currCls == long.class) {
                            //WARN: argsJson.getLong(k + defValue) will return a bigger incorrect number
                            values[currIndex] = Long.parseLong(argsVals.getString(currIndex - 1));
                        } else {
                            values[currIndex] = argsVals.getDouble(currIndex - 1);
                        }
                        numIndex /= 10;
                    }
                }

                return getJsonResult(jsonStr, 200, currMethod.invoke(null, values));
            } catch (Exception e) {
                //优先返回详细的错误信息
                if (e.getCause() != null) {
                    return getJsonResult(jsonStr, 500, "method execution error:" + e.getCause().getMessage());
                }
                return getJsonResult(jsonStr, 500, "method execution error:" + e.getMessage());
            }
        } else {
            return getJsonResult(jsonStr, 500, "call data empty");
        }
    }

    @SuppressLint("DefaultLocale")
	private String getJsonResult (String reqJson, int stateCode, Object result) {
        String insertRes;
        if (result == null) {
            insertRes = "null";
        } else if (result instanceof String) {
            result = ((String) result).replace("\"", "\\\"");
            insertRes = "\"" + result + "\"";
        } else if (!(result instanceof Integer)
                && !(result instanceof Long)
                && !(result instanceof Boolean)
                && !(result instanceof Float)
                && !(result instanceof Double)
                && !(result instanceof JSONObject)) {    // 非数字或者非字符串的构造对象类型都要序列化后再拼接
            if (mGson == null) {
                mGson = new Gson();
            }
            insertRes = mGson.toJson(result);
        } else {  //数字直接转化
            insertRes = String.valueOf(result);
        }
        String resStr = String.format(RETURN_RESULT_FORMAT, stateCode, insertRes);
        Log.d(TAG, mBridgeName + " call json: " + reqJson + " result:" + resStr);
        return resStr;
    }
}