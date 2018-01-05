package com.oss.model;

import com.jfinal.plugin.activerecord.Model;


/**
 * 
 * 仓储环境实时实体类
 * @author ZGW
 *
 */
public class StorageReal extends Model<StorageReal>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final StorageReal dao = new StorageReal().dao();
	
	public StorageReal getStorageByDeviceId(int deviceId){
		return this.findFirst("select * from storagereal where deviceId=?",deviceId);
	}
}
