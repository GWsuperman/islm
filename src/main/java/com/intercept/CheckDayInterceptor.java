package com.intercept;

import java.util.ArrayList;
import java.util.List;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.eova.common.Easy;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.DayRecord;

/**
 * 点检信息每日拦截器
 * @author ZGW
 * @date 2017年11月21日 
 * @version 1.0 
 */
/**
 * @author ZGW
 * @date 2017年11月22日 
 * @version 1.0 
 */
public class CheckDayInterceptor extends MetaObjectIntercept{
	//存储MonthInfo list
	private List<DayRecord> lists = new ArrayList<DayRecord>();
	@Override
    public String addBefore(AopContext ac) throws Exception {
		//清除
		lists.clear();
		Record record = ac.record;
		//从数据库查找判断是否已有该月份信息
		int num = Db.queryInt("select count(*) from day_info where moth_info = ? and c_id = ?",record.get("moth_info"),record.get("c_id"));
		if(num>0){
			return Easy.error("已有该月份！");
		}
		for(int i=1;i<=31;i++){
			String state = record.get("v_"+i);
			if(state!=null && !state.equals("")){
				DayRecord dayInfo = new DayRecord();
				dayInfo.set("day_info",i);
				dayInfo.set("state",state);
				lists.add(dayInfo);
			}
			record.remove("v_"+i);
		}
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
    	for(DayRecord list:lists){
    		list.set("md_id",id);
    		list.save();
    	}
        return null;
    }
    
    /**
     * 查询后置任务
     * <pre>
     * ac.records    获取查询数据集合
     * -------------
     * // 遍历数据集，进行数据操作
     * for (Record x : ac.records){
     *
     *    x.set("total", x.getInt("a") + x.getInt("b"));// 动态计算，total为虚拟字段
     *    x.set("price", String.format("%.2f", price)); // RMB格式化
     *
     * }
     * </pre>
     */
    public void queryAfter(AopContext ac) throws Exception {
    	List<Record> records = ac.records;
    	for(Record x:records){
    		int id = x.get("id");
    		List<DayRecord> lists= DayRecord.dao.find("select * from day_record where md_id=?",id);
    		for(DayRecord list:lists){
    			String i = list.get("day_info");
    			String state = list.getStr("state");
    			x.set("v_"+i,state);
    		}
    	}
    }

    /**
     * 更新查询数据显示在页面的拦截器
     */
    public void updateInit(AopContext ac) throws Exception {
		 Record record = ac.record;
		 int id = record.get("id");
		List<DayRecord> lists= DayRecord.dao.find("select * from day_record where md_id=?",id);
		for(DayRecord list:lists){
			String i = list.get("day_info");
			String state = list.getStr("state");
			record.set("v_"+i,state);
		}
   }
    /**
	 *拦截更新创建点检月份信息
     * </pre>
     */
	@Override
    public String updateBefore(AopContext ac) throws Exception {
		Record record = ac.record;
		int id =  record.get("id");
		for(int i=1;i<=31;i++){
			String state = record.get("v_"+i);
			if(state!=null && !state.equals("")){
				DayRecord dayInfo = DayRecord.dao.findFirst("select * from day_record where md_id = ? and day_info = ?",id,i);
				boolean flag = true;
				if(dayInfo==null){
					dayInfo = new DayRecord();
					flag = false;
				}
				dayInfo.set("day_info",i);
				dayInfo.set("state",state);
				dayInfo.set("md_id",id);
				if(flag){
					dayInfo.update();
				}else{
					dayInfo.save();	
				}
				
			}
			record.remove("v_"+i);
		}
		return null;
	}
	  /**
     * 删除前置任务(事务内)
     * 用法删除关联对象：每天的记录day_record
     * </pre>
     */
    public String deleteBefore(AopContext ac) throws Exception {
    	int id = ac.record.get("id");
    	//删除关联对象
    	deleteRelDayRecord(id);
    	return null;
    }
    
    /**
     * 传入day_info的id删除关联对象day_record
     * @param id
     */
    public static void deleteRelDayRecord(int id){
    	List<DayRecord> dayRecords = DayRecord.dao.find("select id from day_record where md_id = ?",id);
		for(DayRecord dayRecord:dayRecords){
			dayRecord.delete();
		}
    }
}
