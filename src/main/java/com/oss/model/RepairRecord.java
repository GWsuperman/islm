package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * 设备维修记录
 * @author ZGW
 *
 */
public class RepairRecord extends Model<RepairRecord>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final RepairRecord dao = new RepairRecord().dao();
	
	
	
}
