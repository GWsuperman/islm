package com.intercept;


import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.DeviceList;

/**
 * 
 * 
 * 维修记录模块
 * @author ZGW
 * @date 2017年11月17日 
 * @version 1.0 
 */
public class RepairRecordIntercept extends MetaObjectIntercept{
	
    /**
  	 * 查询前置任务(DIY复杂查询条件)
  	 * 
  	 *
  	 * </pre>
  	 */
    @Override
    public void queryAfter(AopContext ac) throws Exception {
    	  for (Record x : ac.records){
			 	int deviceId = x.getInt("deviceId");
			 	 DeviceList d = DeviceList.dao.findById(deviceId);
			 	 if(d!=null){
				 	String deviceNum = d.getStr("devicenum");
				 	String position = d.get("deviceposition");
				 	x.set("v_devicenum",deviceNum);
				 	x.set("v_position",position);
			 	 }
		  }
      }
  
}
