package com.oss.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.Devices;
import com.utils.HttpUtils;

/**
 * agv位置状态service
 * @author ZGW
 * @date 2017年11月7日 
 * @version 1.0 
 */
public class DataVService {
	// 设置请求头
		private static Map<String, String> headers = new HashMap<String, String>();

		{ 
			headers.put("Content-Type", "application/json");
			headers.put("Authorization", "token " + ownerKey + "");
		}
		private static String ownerKey = "e2f78f28a418e045b5a3b8c36cd385e415c92326";
		
		public List<Record> getAgvRealInfo(){
			String url = "https://iot.espressif.cn/v1/device/ping/";
			JSONObject jsonObject = HttpUtils.doGet(url, headers);
			List<Record> agvs = new ArrayList<Record>();
			List<Devices> lists = Devices.dao.findDevicesByType("3");
			//是否在线
			if (jsonObject.containsKey("is_online")) {
				boolean flag = jsonObject.getBoolean("is_online");
				if (flag) {
					agvs = handelAgv(lists);
				}else{
					// 最后一次在线时间
					long lastDate = jsonObject.getDate("last_active").getTime() + 1000 * 60 * 5;
					long newDate = new Date().getTime();
					// 如果当前时间还是大于最后一次在线时间加上五分钟，说明掉线了or关机
					if (lastDate < newDate) {
						lastDate = lastDate + 1000 * 60 * 10;
						//如果还是小于最后在线时间加上30分钟说明关机
						if(lastDate<newDate){
							 //关机
							for (Devices device : lists) {
								Record agv =  commOrShutdownAgv("1");
								agv.set("deviceName",device.getStr("deviceName"));
								agvs.add(agv) ;
							}
						}else{
							 //通讯异常
							for (Devices device : lists) {
								Record agv =  commOrShutdownAgv("0");
								agv.set("deviceName",device.getStr("deviceName"));
								 agvs.add(agv);
							}
						}
					}
				}
			}
			return agvs;
		}
		
		public List<Record> handelAgv(List<Devices> lists){
			//存储agv小车数据
			List<Record> agvLists = new ArrayList<Record>();
			for (Devices device : lists) {
				// 数据模型编号
				String deviceNum = device.get("deviceNum");
				String[] arr = deviceNum.split("-");
				String url = "https://iot.espressif.cn/v1/datastreams/" + arr[0]
						+ "/datapoints/?row_count=1";
				JSONObject jsonobject = HttpUtils.doGet(url, headers);
				if (jsonobject.getString("status").equals("200")) {
					JSONArray jsonData = jsonobject.getJSONArray("datapoints");
					if (jsonData.size() > 0) {
						JSONObject jsonPoint = jsonData.getJSONObject(0);

						// iot上更新日期
						long at = jsonPoint.getDate("at").getTime() + 1000 * 60 * 5;
						// 当期日期
						long currentDate = new Date().getTime();
						// 如果当前时间还是大于最后一次在线时间加上五分钟，说明掉线了
						if (at < currentDate) {
							//再加上30分钟
							 at = at + 1000 * 60 *30;
							 //如果当前时间还是大于最后一次在线时间加上15分钟，关机
							 if (at < currentDate){
								 //关机
								 Record agv =  commOrShutdownAgv("1");
								agv.set("deviceName",device.getStr("deviceName"));
								 agvLists.add(agv) ;
							 }else{
								 //通讯异常
								 Record agv =  commOrShutdownAgv("0");
								 agv.set("deviceName",device.getStr("deviceName"));
								 agvLists.add(agv) ;
							 }
						} else {
							// 设备id
							Record agv = new Record();
							// 运行状态
							int runningState = jsonPoint.getInteger("x");
							// 0关机1开机
							if (runningState == 1) {
								agv.set("runningState", "run");
								agv.set("rdepict", "运行");
							} else {
								agv.set("runningState", "standby");
								agv.set("rdepict", "待机");
							}
							// 异常状态
							int abnormalState = jsonPoint.getInteger("y");
							if (abnormalState == 1) {
								agv.set("abnormalState", "normal");
								agv.set("adepict", "正常");
							} else {
								agv.set("abnormalState", "error");
								agv.set("adepict", "异常");
							}
							// 位置
							String position = jsonPoint.getInteger("z").toString();
							agv.set("position", position);
							agv.set("deviceName",device.getStr("deviceName"));
							 agvLists.add(agv) ;
						}
					}
				}
			}
			return agvLists;
		}
		/**
		 * agv小车处理
		 * flag=0通讯异常，flag=1关机
		 * @param device
		 */
		public Record commOrShutdownAgv(String flag) {
			Record agv = new Record();
			if("0".equals(flag)){
				agv.set("runningState", "comerror");
				agv.set("rdepict", "通讯异常");
				agv.set("abnormalState", "comerror");
				agv.set("adepict", "通讯异常");
			}else{
				agv.set("runningState", "shutdown");
				agv.set("rdepict", "关机");
				agv.set("abnormalState", "shutdown");
				agv.set("adepict", "关机");
			}
			return agv;
		}
	
}
