package com.intercept;

import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;

/**
 * 异常通知模块
 * 通过用户id拦截
 * @author ZGW
 * @date 2017年11月13日 
 * @version 1.0 
 */
public class ByUserIdIntercept extends MetaObjectIntercept{
	 /**
		 * 查询前置任务(DIY复杂查询条件)
		 * 
		 * <pre>
		 * 用法：获取查询条件值
		 * ac.ctrl.getPara("query_查询条件字段名");
		 *
		 * 用法：追加条件
		 * ac.condition = "and id < ?";
		 * ac.params.add(999);
		 *
		 * 用法：覆盖条件
		 * ac.where = "where id < ?";
		 * ac.params.add(5);
		 *
		 * 用法：覆盖排序
		 * ac.sort = "order by id desc";
		 * 
		 * 用法：覆盖整个SQL语句
		 * ac.sql = "select * from table";
		 * </pre>
		 */
	@Override
   public void queryBefore(AopContext ac) throws Exception {
		int lv = ac.user.role.getInt("lv");
		ac.condition = "and lv>=?";
		ac.params.add(lv);
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
    public String addBefore(AopContext ac) throws Exception {
    	ac.record.set("lv",ac.user.role.getInt("lv"));
    	ac.record.set("creator",ac.user.getStr("login_id"));
        return null;
    }
}
