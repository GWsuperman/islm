package com.oss.model;

import com.jfinal.plugin.activerecord.Model;


/**
 * 
 * 仓储环境历史实体类
 * @author ZGW
 *
 */
public class StorageHJ extends Model<StorageHJ>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final StorageHJ dao = new StorageHJ().dao();
}
