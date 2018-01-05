package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * qct设备维保清单
 * @author ZGW
 * @date 2017年11月17日 
 * @version 1.0 
 */
public class DeviceList extends Model<DeviceList>{

	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final DeviceList dao = new DeviceList().dao();
}
