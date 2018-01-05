package com.oss.model;

import com.jfinal.plugin.activerecord.Model;


/**
 * agv小车历史实体类
 * @author ZGW
 *
 */
public class AgvHistory extends Model<AgvHistory>{

	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final AgvHistory dao = new AgvHistory().dao();
	
}
