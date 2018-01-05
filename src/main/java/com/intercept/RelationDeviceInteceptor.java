package com.intercept;

import java.util.List;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.CheckInfo;
import com.oss.model.DeviceList;

/**
 * 
 * 
 * 设备维保模块
 * @author ZGW
 * @date 2017年11月17日 
 * @version 1.0 
 */
public class RelationDeviceInteceptor extends MetaObjectIntercept{
	
	  /**
     * 删除前置任务(事务内)
     * 用法删除关联对象：设备点检信息、月分、每天等信息
     * </pre>
     */
    public String deleteBefore(AopContext ac) throws Exception {
    	int id = ac.record.get("id");
    	//删除关联对象check_info
    	deleteRelCheckInfo(id);
        return null;
    }

    /**
     * 传入设备维保管理id删除关联对象check_info
     * @param id
     */
    public static void deleteRelCheckInfo(int id){
    	//通过维保管理id获取关联点检信息
    	List<CheckInfo> lists = CheckInfo.dao.find("select id from check_info where dmId =?",id);
    	for(CheckInfo list:lists){
    		int cid = list.getInt("id");
    		//删除关联对象month_info
    		CheckMothInterceptor.deleteRelMonth(cid);
        	//删除关联对象day_info
    		CheckMothInterceptor.deleteRelDayInfo(cid);
    		list.delete();
    	}
    }
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
				 	String deviceNum = d.getStr("deviceNum");
				 	x.set("v_devicenum",deviceNum);
			 	 }
		  }
      }
    /**
	 * 新增前置任务(事务内)
	 * 
	 * <pre>
	 * ac.record 当前操作数据
	 * -------------
	 * 用法：自动赋值
	 * ac.record.set("create_time", TimestampUtil.getNow());
	 * ac.record.set("create_uid", ac.UID);
	 *
	 * 用法：唯一值判定
	 * int count = Db.queryInt("select count(*) from users where name = ?", ac.record.getStr("name"));
	 * if (count > 0) {
	 *     return Easy.error("名字不能重复");
	 * }
	 * </pre>
	 */
	@Override
    public String addBefore(AopContext ac) throws Exception {
    	ac.record.remove("v_devicenum");
        return null;
    }
    /**
     * 更新前置任务(事务内)
     * <pre>
     * ac.record 当前操作数据
     * -------------
     * 用法：自动赋值
     * ac.record.set("update_time", new Date());
     * ac.record.set("user_id", ac.user.get("id"));
     *
     * 用法：唯一值判定
     * int count = Db.queryInt("select count(*) from users where name = ?", ac.record.getStr("name"));
     * if (count > 0) {
     *     return Easy.error("名字被占用了");
     * }
     * </pre>
     */
	@Override
    public String updateBefore(AopContext ac) throws Exception {
    	ac.record.remove("v_devicenum");
        return null;
    }
}
