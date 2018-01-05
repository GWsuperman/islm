package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * 点检月份信息
 * @author ZGW
 * @date 2017年11月21日 
 * @version 1.0 
 */
public class MonthInfo extends Model<MonthInfo>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final MonthInfo dao = new MonthInfo();
	
	/**
	 * 通过点检id和月份获取点检月份信息
	 * @param cid
	 * @param month
	 * @return
	 */
	public MonthInfo getInfo(int cid,int month){
		return dao.findFirst("select * from month_info where c_id = ? and month_info = ?",cid,month);
	}
}
