package com.aofeng.hybrid.sync;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.aofeng.hybrid.util.CommUtil;
import com.aofeng.hybrid.util.LogUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

/**
 * 同步离线网站文件和数据库
 * @author LGY
 *
 */
public class AlignDBAndPagesTask extends AsyncTask<String, Integer, Boolean>{
	private IProgressNotifier progressNotifier;
	private Date lastModifiedDate;
    private Context context;
    public static String UPDATE_MOST_RECENT_TIME = "UPDATE_MOST_RECENT_TIME";

    /**
	 * 
	 * @param pn  进度变化时调用
	 */
	public AlignDBAndPagesTask(Context context, IProgressNotifier pn)
	{
        this.context = context;
		progressNotifier = pn;
	}
	
	/**
	 * 异步同步工作
	 */
	@Override
	protected Boolean doInBackground(String... urls) {
		try
		{
			//检查是否需要同步    version url, last modified date
			if(!needsSync(urls[7], urls[8]))
				return false;
			//      url, tables
			alignDB(urls[1], urls[2]);
			//提取页面url，远程目录, 本地目录, 下载服务url
			alignPages(urls[3], urls[4], urls[5], urls[6]);
			
			//同步成功
			if(lastModifiedDate == null)
				lastModifiedDate = new Date();
			CommUtil.savePreference(context, urls[0], UPDATE_MOST_RECENT_TIME, (lastModifiedDate.getTime()+""));
			return Boolean.valueOf(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return Boolean.valueOf(false);
		}
	}

	@SuppressLint("SimpleDateFormat")
	private boolean needsSync(String url, String dt) throws Exception {
		if(dt == null)
			return true;
		HttpGet getMethod = new HttpGet(url);
		HttpClient httpClient = CommUtil.getTimeoutHttpClient();
		HttpResponse res = httpClient.execute(getMethod);
		int code = res.getStatusLine().getStatusCode();
		if (code == 200){
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			JSONObject jo = new JSONObject(EntityUtils.toString(res.getEntity(), "UTF-8"));
			LogUtil.d("Async server timestamp", jo.getString(("value")));
			LogUtil.d("Async client timestamp", sdf.format(new Date(Long.parseLong(dt))));
			lastModifiedDate = sdf.parse(jo.getString("value"));
			if(new Date(Long.parseLong(dt)).before(lastModifiedDate))
				return true;
			else
				return false;
		}
		return false;
	}

	/**
	 * 同步页面
	 * @param fetchUrl
	 * @throws Exception 
	 */
	private void alignPages(String fetchUrl, String remoteDir, String localDirPrefix, String fileDownloadUrl) throws Exception {
		HttpGet getMethod = new HttpGet(fetchUrl);
		HttpClient httpClient = CommUtil.getTimeoutHttpClient();
		HttpResponse res = httpClient.execute(getMethod);
		int code = res.getStatusLine().getStatusCode();
		if (code == 200){
			FileHelper.makeRootDir(localDirPrefix);
			String result = EntityUtils.toString(res.getEntity(), "UTF-8");
			String[] files = result.split("\\|", -1);
			int i = 0;
			int n = files.length;
			for(String file : files)
			{
				Log.d("同步文件", "同步文件:" + file);
				updateFile(file, remoteDir, localDirPrefix,fileDownloadUrl);
				publishProgress(50 + (i*50)/n);
				i++;
			}
			publishProgress(100);
		}
		else
			throw new Exception("同步文件出错。");
		
	}

	/**
	 * 更新单个文件
	 * @param fileNameAndDate
	 * @throws Exception 
	 */
	private void updateFile(String fileNameAndDate, String remoteDir, String localDirPrefix, String fileDownloadUrl) throws Exception {
		String[] pair = fileNameAndDate.split(",");
		//去掉无关目录
		String path = pair[0].replace(remoteDir, "");
		long timestamp = Long.parseLong(pair[1]);
		FileHelper.processFile(localDirPrefix, path, timestamp, remoteDir, fileDownloadUrl);
	}

	/**
	 * 生成或更新表
	 * @param jo
	 * @throws Exception 
	 */
	private void createOrUpdateTable(String tableName, JSONObject jo) throws Exception {
		if(!DBHelper.hasTable(context, tableName))
			DBHelper.createTable(context, tableName, jo);
		else
			DBHelper.updateTable(context, tableName, jo);
	}

	/**
	 * 同步数据库
	 * @param url
	 */
	private void alignDB(String url, String tables) throws Exception {
		HttpPost postMethod = new HttpPost(url);
		HttpClient httpClient = CommUtil.getTimeoutHttpClient();
		postMethod.setEntity(new StringEntity(tables,"UTF-8"));
		HttpResponse res = httpClient.execute(postMethod);
		int code = res.getStatusLine().getStatusCode();
		if (code == 200){
			DBHelper.createDBIfNotExist(context);
			JSONObject jo = new JSONObject(EntityUtils.toString(res.getEntity(), "UTF-8"));
			@SuppressWarnings("unchecked")
			Iterator<String> iter = jo.keys();
			int n = jo.length();
			int i = 0;
			while (iter.hasNext()){
				String tableName = iter.next();
				Log.d("同步表：", "处理表:" + tableName);
				createOrUpdateTable(tableName.replace(".", "_"), jo.getJSONObject(tableName));
				publishProgress((i*50)/n);
				i++;
			}
			publishProgress(50);
		}
		else
			throw new Exception("同步数据库出错。");
	}

	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressNotifier.prelude();
	}

	/**
	 * 同步完成
	 */
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		progressNotifier.notifyDone(result);
	}

	/**
	 * 通知进度
	 */
	@Override
	protected void onProgressUpdate(Integer... progress) {
		progressNotifier.notifyProgress(progress[0]);
	}
}
