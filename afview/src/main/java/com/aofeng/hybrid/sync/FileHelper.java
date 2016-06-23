package com.aofeng.hybrid.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * 文件处理类
 * @author LGY
 *
 */
public class FileHelper {

	/**
	 * 处理传入的文件
	 * @param path  包含路径的文件名
	 * @param timestamp 文件最后修改日期
	 * @throws Exception 
	 */
	public static void processFile(String localDirPrefix, String path, long timestamp, String remoteDir,String fileDownloadUrl) throws Exception {
		path = path.replace("\\", "/");
		File file = new File(localDirPrefix + path);
		
		int pos = path.lastIndexOf("/");
		
		//如果文件不存在
		if(!file.exists())
		{
			if(pos != -1)
			{
				File fullPath = new File(localDirPrefix + path.substring(0, pos));
				fullPath.mkdirs();
			}
			file.createNewFile();
		}
		
		long lastModifedTime = file.lastModified();
		//如果文件最后修改时间不一致，下载
		if(Math.abs(lastModifedTime - timestamp) > 1000)
		{
			file.createNewFile();
			downloadFile(fileDownloadUrl, file, remoteDir + path.replace("/", "\\"));
			file.setLastModified(timestamp);
		}
	}

	/**
	 * 下载文件
	 * @param file
	 * @param path
	 * @throws Exception 
	 * @throws  
	 */
	private static void downloadFile(String fileDownloadUrl, File file, String path) throws Exception {
		URL url = new URL(fileDownloadUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.getOutputStream().write(path.getBytes("UTF-8"));
		InputStream is=conn.getInputStream();
		OutputStream os = new FileOutputStream(file);
		byte buf[] = new byte[1024];
	    do
	    {
	        int numread = is.read(buf);
	        if (numread == -1)
	        {
	          break;
	        }
	        os.write(buf, 0, numread);
	    }while(true);
		is.close();
		os.close();
	}

	/**
	 * 建www根目录
	 */
	public static void makeRootDir(String localDirPrefix) {
		File fullPath = new File(localDirPrefix);
		if(!fullPath.exists())
			fullPath.mkdirs();
	}

}
