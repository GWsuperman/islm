package com.oss.model;

import com.jfinal.plugin.activerecord.Model;


/**
 * 空压机实时状态
 * @author Administrator
 *
 */
public class ComState extends Model<ComState>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final ComState dao = new ComState().dao();
	
	/**
	 * 根据设备id查找空压机房设备信息
	 * @param deviceId
	 * @return
	 */
	public ComState getComStateDeviceId(Integer deviceId){
		return this.findFirst("select * from comstate where deviceId=?",deviceId);
	}
}
