package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * 设备维保管理
 * @author ZGW
 *
 */
public class MainManage extends Model<MainManage>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final MainManage dao = new MainManage().dao();
	
	
	
}
