package com.aofeng.hybrid.plugin;

import java.io.Serializable;

public class H5Param implements Serializable{

	/**
	 * 串行版本号
	 */
	private static final long serialVersionUID = 1L;
	
	public String page;
	public String clazz;

	public H5Param(String page, String clazz)
	{
		this.page = page;
		this.clazz = clazz;
	}
	
}
