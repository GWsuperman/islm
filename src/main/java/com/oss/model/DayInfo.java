package com.oss.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * 月日关联信息
 * @author ZGW
 * @date 2017年11月22日 
 * @version 1.0 
 */
public class DayInfo extends Model<DayInfo>{

	private static final long serialVersionUID = 1064291771401662738L;
	public static final DayInfo dao = new DayInfo().dao();

}
