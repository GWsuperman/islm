package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * 点检信息实体
 * @author ZGW
 *
 */
public class CheckInfo extends Model<CheckInfo>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final CheckInfo dao = new CheckInfo().dao();
	
	
	
}
