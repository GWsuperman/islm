package com.oss.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Model;


/**
 * agv小车实时实体类
 * @author ZGW
 *
 */
public class AgvReal extends Model<AgvReal>{

	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final AgvReal dao = new AgvReal().dao();
	
	/**
	 * 根据设备id查找agv信息
	 * @param deviceId
	 * @return
	 */
	public AgvReal getAgvRealByDeviceId(Integer deviceId){
		return this.findFirst("select a.*,d.deviceName from agvreal a,devices d where d.id = a.deviceId and a.deviceId=?",deviceId);
	}
	
	/**
	 * 查询所有agv小车实时状态信息
	 * @return
	 */
	public List<AgvReal> findAllAgv(){
		return this.find("select a.*,d.deviceName from agvreal a left join devices d on d.id = a.deviceId");
	}
}
