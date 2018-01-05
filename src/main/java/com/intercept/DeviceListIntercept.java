package com.intercept;

import java.util.Date;
import java.util.List;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.eova.common.Easy;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.MainManage;
import com.oss.model.RepairRecord;

/**
 * 
 * qct设备清单模块拦截器
 * @author ZGW
 * @date 2017年11月17日 
 * @version 1.0 
 */
public class DeviceListIntercept extends MetaObjectIntercept{
	
    /**
     * 新增页初始化
     * <pre>
     * ac.fixed        当前操作对象固定初始值
     * -------------
     * 用法：初始化默认值
     * ac.fixed("reply", "您好，");// 回复内容给统一前缀
     * </pre>
     */
    public void addInit(AopContext ac) throws Exception {
    }
    
	  /**
		 * 新增前置任务(事务内)
		 *新增设备清单时后台添加创建日期
		 */
		@Override
	    public String addBefore(AopContext ac) throws Exception {
			ac.record.set("createDate",new Date());
			 int count2 = Db.queryInt("select count(*) from device_list where deviceNum = ?", ac.record.get("devicenum"));
			 if (count2 > 0) {
			     return Easy.error("设备编号不能重复!");
			 }
			int count = Db.queryInt("select count(*) from device_list where deviceName = ?", ac.record.get("devicename"));
			 if (count > 0) {
			     return Easy.error("设备名称不能重复!");
			 }
			
	        return null;
	    }
		
	    /**
	     * 删除前置任务(事务内)
	     * 
	     * 通过设备清单id删除关联对象device_manage(维保管理) and repair_record(维修记录)
	     */
	    public String deleteBefore(AopContext ac) throws Exception {
	    	int id = ac.record.get("id");
	    	//删除维保管理
	    	deleteRelDeviceManage(id);
	    	//删除维修记录
	    	deleteRelRepair(id);
	        return null;
	    }
	    
	    /**
	     * 通过设备清单id删除关联对象device_manage
	     * @param id
	     */
	    public void deleteRelDeviceManage(int id){
	    	List<MainManage>  records = MainManage.dao.find("select id from device_manage where deviceId = ?",id);
	    	for(MainManage record:records){
	    		int dmId = record.getInt("id");
	    		//删除关联对象check_info
	    		RelationDeviceInteceptor.deleteRelCheckInfo(dmId);
	    		record.delete();
	    	}
	    }
	    
	    /**
	     * 通过设备维保清单id删除关联对象repair_record
	     * @param id
	     */
	    public void deleteRelRepair(int id){
	    	List<RepairRecord> lists = RepairRecord.dao.find("select id from repair_record where deviceId = ?",id);
	    	for(RepairRecord list:lists){
	    		list.delete();
	    	}
	    }
}
