package com.intercept;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.Devices;

/**
 * 设备拦截器，
 * @author ZGW
 * @date 2017年11月10日 
 * @version 1.0 
 */
public class DeviceIntercept extends MetaObjectIntercept{

	/**
	 * 后置查询拦截器
	 * 	拦截根据字段deviceId外键获取设备表区域、设备名称等信息。
	 * 
	 */
	@Override
	 public void queryAfter(AopContext ac) throws Exception {
		 for (Record x : ac.records){
			 	int deviceId = x.getInt("deviceId");
			 	 Devices d = Devices.dao.findById(deviceId);
			 	String deviceName =  d.getStr("deviceName");
			 	String region = d.getStr("region");
			 	x.set("v_region",region);
			 	x.set("v_deviceName", deviceName);
		  }
    }
}
