/**
 * Copyright (c) 2013-2016, Jieven. All rights reserved.
 *
 * Licensed under the GPL license: http://www.gnu.org/licenses/gpl.txt
 * To use it on other terms please contact us at 1623736450@qq.com
 */
package com.oss;

import java.util.HashMap;

import com.eova.config.EovaConfig;
import com.eova.core.IndexController;
import com.eova.core.auth.AuthController;
import com.eova.core.button.ButtonController;
import com.eova.core.menu.MenuController;
import com.eova.core.meta.MetaController;
import com.eova.core.task.TaskController;
import com.eova.interceptor.AuthInterceptor;
import com.eova.interceptor.LoginInterceptor;
import com.eova.template.common.config.TemplateConfig;
import com.eova.template.masterslave.MasterSlaveController;
import com.eova.template.office.OfficeController;
import com.eova.template.single.SingleController;
import com.eova.template.singlechart.SingleChartController;
import com.eova.template.singletree.SingleTreeController;
import com.eova.template.treetogrid.TreeToGridController;
import com.eova.user.UserController;
import com.eova.widget.WidgetController;
import com.eova.widget.tree.TreeController;
import com.eova.widget.treegrid.TreeGridController;
import com.eova.widget.upload.UploadController;
import com.jfinal.config.Constants;
import com.jfinal.config.Interceptors;
import com.jfinal.config.Plugins;
import com.jfinal.config.Routes;
import com.jfinal.config.Routes.Route;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.oss.dataV.DataVController;
import com.oss.model.AgvHistory;
import com.oss.model.AgvReal;
import com.oss.model.CheckInfo;
import com.oss.model.ComState;
import com.oss.model.Compressor;
import com.oss.model.DayInfo;
import com.oss.model.DayRecord;
import com.oss.model.DeviceList;
import com.oss.model.DeviceTrigger;
import com.oss.model.Devices;
import com.oss.model.Liaisons;
import com.oss.model.LsxHistory;
import com.oss.model.LsxReal;
import com.oss.model.MailInfo;
import com.oss.model.MainManage;
import com.oss.model.MonthInfo;
import com.oss.model.RepairRecord;
import com.oss.model.StorageHJ;
import com.oss.model.StorageReal;
import com.oss.product.ProductController;
import com.oss.service.IotAPIService;

public class OSSConfig extends EovaConfig {

	@Override  
	public void configConstant(Constants me) {  
	    // TODO Auto-generated method stub  
		super.configConstant(me);
	}  

	/**
	 * 自定义路由
	 *
	 * @param me
	 */
	@Override
	protected void route(Routes me) {
		// 自定义的路由配置往这里加。。。
		me.add("/user", UserController.class);

		me.add("/", OSSController.class);
		me.add("/product", ProductController.class);
		me.add("/dataV",DataVController.class);
		me.add("/iotTriService",IotAPIService.class);
		// 排除不需要登录拦截的URI 语法同SpringMVC拦截器配置 @see com.eova.common.utils.util.AntPathMatcher
		LoginInterceptor.excludes.add("/init");
		LoginInterceptor.excludes.add("/code");
		LoginInterceptor.excludes.add("/test");
		// LoginInterceptor.excludes.add("/xxxx/**");
	}
	/**
	 * 配置路由
	 */
	@Override
	public void configRoute(Routes me) {
		System.err.println("Config Routes Starting...");

		// 业务模版
		me.add("/" + TemplateConfig.SINGLE_GRID, SingleController.class);
		me.add("/" + TemplateConfig.SINGLE_TREE, SingleTreeController.class);
		me.add("/" + TemplateConfig.SINGLE_CHART, SingleChartController.class);
		me.add("/" + TemplateConfig.MASTER_SLAVE_GRID, MasterSlaveController.class);
		me.add("/" + TemplateConfig.TREE_GRID, TreeToGridController.class);
		me.add("/" + TemplateConfig.OFFICE, OfficeController.class);
		// 组件
		me.add("/widget", WidgetController.class);
		me.add("/upload", UploadController.class);
//		me.add("/form", FormController.class);
		me.add("/form", MyFormController.class);
		me.add("/grid", MyGridController.class);
		me.add("/tree", TreeController.class);
		me.add("/treegrid", TreeGridController.class);
		// Eova业务
		me.add("/meta", MetaController.class);
		me.add("/menu", MenuController.class);
		me.add("/button", ButtonController.class);
		me.add("/auth", AuthController.class);
		me.add("/task", TaskController.class);
		// me.add("/cloud", CloudController.class);

		LoginInterceptor.excludes.add("/cloud");

		// 自定义路由
		route(me);

		// 如果有自定义，将不再注册系统默认实现
		boolean flag = false;
		for (Route x : me.getRouteItemList()) {
			if (x.getControllerKey().equals("/")) {
				flag = true;
			}
		}
		if (!flag)
			me.add("/", IndexController.class);
	}
	/**
	 * 自定义Main数据源Model映射
	 *
	 * @param arp
	 */
	@Override
	protected void mapping(HashMap<String, ActiveRecordPlugin> arps) {
//		 获取主数据源的ARP
		 ActiveRecordPlugin main = arps.get("main");
		 //监测设备
		 main.addMapping("devices",Devices.class);
		 //agv历史
		 main.addMapping("agvhistory",AgvHistory.class);
		 //agv实时
		 main.addMapping("agvreal", AgvReal.class);
		 //空压机历史
		 main.addMapping("compressor",Compressor.class);
		 //空压机实时
		 main.addMapping("comstate",ComState.class);
		 //环境监测历史
		 main.addMapping("storagehj",StorageHJ.class);
		 //环境监测实时
		 main.addMapping("storagereal",StorageReal.class);
		 //联系人
		 main.addMapping("liaisons_info",Liaisons.class);
		 //邮件信息
		 main.addMapping("mail_info",MailInfo.class);
		 //流水线历史
		 main.addMapping("lsx_history",LsxHistory.class);
		 //流水线实时
		 main.addMapping("lsx_real",LsxReal.class);
		 //设备维保清单
		 main.addMapping("device_list",DeviceList.class);
		 //点检月份详情
		 main.addMapping("month_info",MonthInfo.class);
		 //点检日信息
		 main.addMapping("day_info",DayInfo.class);
		 //点检每日记录详情
		 main.addMapping("day_record",DayRecord.class);
		 //点检信息
		 main.addMapping("check_info",CheckInfo.class);
		 //维保关联
		 main.addMapping("device_manage",MainManage.class);
		 //维修记录
		 main.addMapping("repair_record",RepairRecord.class);
		 main.addMapping("devicetrigger",DeviceTrigger.class);
		// 自定义业务Model映射往这里加
		// main.addMapping("user_info", UserInfo.class);
		// main.addMapping("users", Users.class);
		// main.addMapping("address", Address.class);
		// main.addMapping("orders", Orders.class);

		// 获取其它数据源的ARP
		// ActiveRecordPlugin xxx = arps.get("xxx");
	}

	/**
	 * 自定义插件
	 */
	@Override
	protected void plugin(Plugins plugins) {
		// 添加需要的插件
	}

	/**
	 * 自定义表达式(主要用于级联)
	 */
	@Override
	protected void exp() {
		super.exp();
		// 区域级联查询
		exps.put("selectAreaByLv2AndPid", "select id ID,name CN from area where lv = 2 and pid = ?");
		exps.put("selectAreaByLv3AndPid", "select id ID,name CN from area where lv = 3 and pid = ?");
		exps.put("selectEovaMenu", "select id,parent_id pid, name, iconskip from eova_menu;ds=eova");
		exps.put("selectDevice","select id ID,deviceNum CN from device_list where id = ?");
		exps.put("triggerCascade","select id ID,attr CN from deviceattr where deviceType=?");
		// 用法，级联动态在页面改变SQL和参数
		// $xxx.eovacombo({exp : 'selectAreaByLv2AndPid,aaa,10'}).reload();
		// $xxx.eovafind({exp : 'selectAreaByLv2AndPid,aaa,10'});
		// $xxx.eovatree({exp : 'selectAreaByLv2AndPid,10'}).reload();
	}

	@Override
	protected void authUri() {
		super.authUri();

		// 放行所有角色,所有URI(我是小白,我搞不明白URI配置,请使用这招,得了懒癌也可以这样搞后果自负.)
		// HashSet<String> uris = new HashSet<String>();
		// uris.add("/**/**");
		// authUris.put(0, uris);

		// 单独放行某角色xxx业务
		// uris.add("/xxx/**");
		// authUris.put(角色ID, uris);

		// URI配置语法咋么写?
		// @see AntPathMatcher
	}

}