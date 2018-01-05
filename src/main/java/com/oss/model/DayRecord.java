package com.oss.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * 点检每日记录
 * @author ZGW
 * @date 2017年11月22日 
 * @version 1.0 
 */
public class DayRecord extends Model<DayRecord>{

	private static final long serialVersionUID = 1064291771401662738L;
	public static final DayRecord dao = new DayRecord().dao();

}
