package com.aofeng.hybrid.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 处理数据库同步
 * @author LGY
 *
 */
public class DBHelper {

	/**
	 * 数据库不存在则创建数据库
	 */
	public static void createDBIfNotExist(Context context) {
		//不存在，则创建
		SQLiteDatabase db = context.openOrCreateDatabase("hybrid.db", Context.MODE_PRIVATE, null);
		db.close();
	}

	/**
	 * 是否存在该表
	 * @param tableName
	 * @return
	 */
	public static boolean hasTable(Context context, String tableName) {
		Cursor cursor = null;
		tableName = tableName.replace(".", "_");
		SQLiteDatabase db = context.openOrCreateDatabase("hybrid.db", Context.MODE_PRIVATE, null);
		cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'", null);
		boolean result = cursor.moveToNext();
		cursor.close();
		db.close();
		return result;
	}

	/**
	 * 创建表
	 * @param tableName
	 * @param jo
	 * @throws JSONException 
	 */
	public static void createTable(Context context, String tableName, JSONObject jo) throws Exception {
		Map<String, String> colMap = getColumnsFromJson(jo);

		String sql = "CREATE TABLE " + tableName + " (" ;
		for(String colName : colMap.keySet())
		{
			sql += colName + " " + colMap.get(colName);
			//如果是id列，作为关键字
			if(colName.toLowerCase().equals("id"))
				sql += " PRIMARY KEY , "; 
			else
				sql += ", ";
		}
		
		//去掉最后的,
		sql = sql.substring(0, sql.length()-2);
		sql += ")";
		
		SQLiteDatabase db = context.openOrCreateDatabase("hybrid.db", Context.MODE_PRIVATE, null);
		db.execSQL(sql);
		db.close();
	}

	/**
	 * 得到服务端数据表各列以及列类型
	 * @param jo
	 * @return
	 * @throws Exception
	 */
	private static Map<String, String> getColumnsFromJson(JSONObject jo) throws Exception {
		Map<String, String> colMap = new HashMap<String, String>();
		@SuppressWarnings("unchecked")
		Iterator<String> itr = jo.keys();
		while(itr.hasNext())
		{
			String name = itr.next();
			name = name.replace(".", "_");
			String type = jo.getString(name);
			//不是一对一、一对多关联
			if(!type.endsWith("[]") && !jo.has(type))
				colMap.put(name, Column.normalizeType(jo.getString(name)));
		}
		return colMap;
	}

	/**
	 * 得到表列和类型
	 * @param tableName
	 * @return
	 */
	private static Map<String, String> getTableColumns(Context context, String tableName)
	{
		Map<String, String> colMap = new HashMap<String, String>();
		SQLiteDatabase db = context.openOrCreateDatabase("hybrid.db", Context.MODE_PRIVATE, null);
		Cursor cursor = db.rawQuery("pragma table_info(" + tableName + ")", null);
		while(cursor.moveToNext())
		{
			colMap.put(cursor.getString(cursor.getColumnIndex("name")), cursor.getString(cursor.getColumnIndex("type")).toUpperCase());			
		}
		cursor.close();
		db.close();
		return colMap;
	}
	
	/**
	 * 更新表，只支持字段添加
	 * The ALTER TABLE command in SQLite allows the user to rename a table or 
	 * to add a new column to an existing table. 
	 * @param tableName
	 * @param jo
	 * @throws Exception 
	 */
	public static void updateTable(Context context, String tableName, JSONObject jo) throws Exception {
		tableName = tableName.replace(".", "_");
		
		Map<String, String> colAndroid = getTableColumns(context, tableName);
		Map<String, String> colRest = getColumnsFromJson(jo);
		
		SQLiteDatabase db = context.openOrCreateDatabase("hybrid.db", Context.MODE_PRIVATE, null);
		for(String rCol : colRest.keySet())
		{
			if(colAndroid.containsKey(rCol))
				continue;
			else
			{
				String sql = "ALTER TABLE " + tableName + " ADD " + rCol + " " + colRest.get(rCol);
				db.execSQL(sql);
			}
		}
		db.close();
	}
}

/**
 * 表的列
 * @author LGY
 *
 */
class Column
{
	public static String normalizeType(String type)
	{
		String tp = type.toLowerCase();
		if(tp.equals("string"))
		{
			return "TEXT";
		}
		else if(tp.equals("integer"))
		{
			return "INTEGER";
		}
		else if(tp.equals("date") || tp.equals("time") || tp.equals("boolean"))
		{
			return "NUMERIC";
		}
		else if(tp.equals("double") || tp.equals("big_decimal"))
		{
			return "REAL";
		}
		else if(tp.equals("blob"))
		{
			return "BLOB";
		}
		else
			return "TEXT";
	}
	
	/**
	 * 是不是字段可以换类型
	 * @param type
	 * @param type2
	 * @return
	 */
	public static boolean isCompatible(String type, String type2)
	{
		return true;
	}
}
