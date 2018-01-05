package com.oss.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Model;

/**
 * 设备实体类
 * @author ZGW
 * @version 1.0
 *
 */
public class Devices extends Model<Devices>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final Devices dao = new Devices().dao();
	
	/**
	 * 根据设备类型获取相关设备
	 * @param deviceType
	 * @return
	 */
	public List<Devices> findDevicesByType(String deviceType){
		return this.findByCache("deviceCache","device"+deviceType, "select * from devices where deviceType=?",deviceType);
	}
	
}
