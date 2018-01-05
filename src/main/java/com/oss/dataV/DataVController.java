package com.oss.dataV;

import java.util.List;

import com.jfinal.aop.Clear;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.AgvReal;
import com.oss.service.DataVService;

public class DataVController extends Controller{
	static DataVService service = new DataVService();
	private AgvReal dao = AgvReal.dao;
	public void index(){
		render("/eova/dataV.html");
	}
	//首次进入小车位置
	public void show(){
		AgvReal a1 = dao.getAgvRealByDeviceId(12);
		AgvReal a2 = dao.getAgvRealByDeviceId(13);
//		List<AgvReal> a = new ArrayList<AgvReal>();
		setAttr("agv1",a1);
		setAttr("agv2",a2);
		render("/eova/show.html");
 	}
	//ajax刷新从iot上获取小车位置
	@Clear
	public void refreshPosition(){
		List<Record> lists = service.getAgvRealInfo();
		setAttr("lists",lists);
		renderJson();
	}
}
