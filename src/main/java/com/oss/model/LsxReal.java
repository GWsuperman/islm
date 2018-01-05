package com.oss.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * 流水线压力监测实时
 * @author ZGW
 * @date 2017年11月14日 
 * @version 1.0 
 */
public class LsxReal extends Model<LsxReal>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final LsxReal dao = new LsxReal().dao();
	
	public LsxReal findByDeviceId(int deviceId){
		return this.findFirst("select * from lsx_real where deviceId=?",deviceId);
	}

}
