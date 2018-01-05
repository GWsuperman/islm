package com.oss.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * 流水线压力监测历史
 * @author ZGW
 * @date 2017年11月14日 
 * @version 1.0 
 */
public class LsxHistory extends Model<LsxHistory>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final LsxHistory dao = new LsxHistory().dao();
	
	public LsxHistory findBydeviceId(int deviceId){
		String sql = "select * from lsx_history where deviceId=? order by createDate desc";
		return this.findFirst(sql,deviceId);
	}

}
