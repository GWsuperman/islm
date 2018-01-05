package com.intercept;

import java.util.ArrayList;
import java.util.List;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.DayInfo;
import com.oss.model.MonthInfo;

/**
 * 点检信息月份拦截器
 * @author ZGW
 * @date 2017年11月21日 
 * @version 1.0 
 */
public class CheckMothInterceptor extends MetaObjectIntercept{
	//存储MonthInfo list
	private List<MonthInfo> lists = new ArrayList<MonthInfo>();
	@Override
    public String addBefore(AopContext ac) throws Exception {
		lists.clear();
		Record record = ac.record;
		for(int i=1;i<=12;i++){
			String state = record.get("v_"+i);
			if(state!=null && !state.equals("")){
				MonthInfo monthInfo = new MonthInfo();
				monthInfo.set("month_info",i);
				monthInfo.set("state",state);
				lists.add(monthInfo);
			}
			record.remove("v_"+i);
		}
		record.remove("v_operation");
        return null;
    }
    /**
     * 新增后置任务(事务内)
     * <pre>
     * ac.record 当前操作数据
     * -------------
     * 用法：级联新增，需在同事务内完成
     * int id = ac.record.getInt("id");// 获取当前对象主键值，进行级联新增
     * </pre>
     */
    public String addAfter(AopContext ac) throws Exception {
    	int id = ac.record.getInt("id");
    	for(MonthInfo list:lists){
    		//每月点检信息
    		list.set("c_id", id);
    		list.save();
    		//月日信息
    		DayInfo dayInfo = new DayInfo();
			dayInfo.set("moth_info",list.get("month_info"));
			dayInfo.set("c_id",id);
			dayInfo.save();
    	}
        return null;
    }
    
	/**
	 *拦截更新创建点检月份信息
     * </pre>
     */
	@Override
    public String updateBefore(AopContext ac) throws Exception {
		Record record = ac.record;
		//点检信息表Id
		int cid =  record.get("id");
		for(int i=1;i<=12;i++){
			String state = record.get("v_"+i);
			if(state!=null && !state.equals("")){
				MonthInfo monthInfo = MonthInfo.dao.getInfo(cid,i);
				boolean flag = true;
				if(monthInfo==null){
					monthInfo = new MonthInfo();
					flag = false;
				}
				monthInfo.set("month_info",i);
				monthInfo.set("state",state);
				monthInfo.set("c_id",cid);
				if(flag){
					monthInfo.update();
				}else{
					monthInfo.save();
					
					//月日点检信息
		    		DayInfo dayInfo = new DayInfo();
					dayInfo.set("moth_info",i);
					dayInfo.set("c_id",cid);
					dayInfo.save();
				}
				
			}
			record.remove("v_"+i);
		}
		record.remove("v_operation");
        return null;
    }
	
    /**
     * 拦截查询后置任务
     * 通过点检信息id和月份获取点检月份信息
     * </pre>
     */
	@Override
    public void queryAfter(AopContext ac) throws Exception {
    	 for (Record x : ac.records){
    		 	//点检信息id
    		 	int id = x.get("id");
    		 	for(int i=1;i<=12;i++){
    		 		MonthInfo monthInfo = MonthInfo.dao.getInfo(id,i);
    		 		if(monthInfo!=null){
    		 			String state = monthInfo.get("state");
    		 			x.set("v_"+i,state);
    		 		}
    		 	}
    		 	 ac.ctrl.setAttr("checkInfo_mid",x.get("mdid"));
    	 }
    }
	
	 public void updateInit(AopContext ac) throws Exception {
		 Record record = ac.record;
		 int id = record.get("id");
		 List<MonthInfo> lists =  MonthInfo.dao.find("select * from month_info where c_id = ?",id);
		 for(MonthInfo list:lists){
			 	String state = list.get("state");
			 	String m = list.get("month_info");
	 			record.set("v_"+m,state);
		 }
 	}
	 
	  /**
	     * 删除前置任务(事务内)
	     * 用法删除关联对象：day_info、month_info、day_record
	     * </pre>
	     */
    public String deleteBefore(AopContext ac) throws Exception {
    	int id = ac.record.get("id");
    	//删除关联对象month_info
    	deleteRelMonth(id);
    	//删除关联对象day_info
    	deleteRelDayInfo(id);
    	return null;
    }
    
    /**
     * 传入点检信息id删除关联对象month_info
     * @param id
     */
    public static void deleteRelMonth(int id){
    	List<MonthInfo> lists = MonthInfo.dao.find("select id from month_info where c_id =?",id);
    	for(MonthInfo list:lists){
    		list.delete();
    	}
    }
    
    /**
     * 传入点检信息id删除关联对象day_info
     * @param id
     */
    public static void deleteRelDayInfo(int id){
    	List<DayInfo> dayInfos = DayInfo.dao.find("select id from day_info where c_id = ?",id);
    	for(DayInfo dayInfo:dayInfos){
    		int dId = dayInfo.getInt("id");
    		//通过DayInfo id删除关联对象DayRecord
    		CheckDayInterceptor.deleteRelDayRecord(dId);
    		dayInfo.delete();
    	}
    }
}
