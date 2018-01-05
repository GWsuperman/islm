package com.oss.job;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oss.model.AgvHistory;
import com.oss.model.Compressor;
import com.oss.model.Devices;
import com.oss.model.LsxHistory;
import com.oss.model.StorageHJ;
import com.utils.HttpUtils;

/**
 * 每小时执行
 *
 * @author ZGW
 * @date 2017年11月16日
 * @version V1.0
 */
public class EveryHourJob extends AbsJob {
	// 设置请求头
	private static Map<String, String> headers = new HashMap<String, String>();

	{
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "token " + ownerKey + "");
	}
	private static String ownerKey = "e2f78f28a418e045b5a3b8c36cd385e415c92326";
	@Override
	protected void process(JobExecutionContext context) {
		// 获取设备类型为：1（空压机房）,2:仓储温湿度，3：agv小车的所有设备信息
				List<Devices> lists1 = Devices.dao.findDevicesByType("1");
				List<Devices> lists2 = Devices.dao.findDevicesByType("2");
				List<Devices> lists3 = Devices.dao.findDevicesByType("3");
				List<Devices> lists4 = Devices.dao.findDevicesByType("4");
				// 处理空压机房数据
				handelKYJF(lists1);
				// 处理仓储温湿度数据
				handlSHJ(lists2);
				// 处理agv小车数据
				handlAgv(lists3);
				//处理流水线压力
				handelLsx(lists4);
	}
	/**
	 * 空压机房设备处理
	 * 
	 * @param device
	 */
	public void commOrShutdwonKYJF(Devices device,String flag) {
		int deviceId = device.get("id");
		Compressor com = new Compressor();
//		Compressor com2 = Compressor.dao.findFirst("select * from compressor order by createDate desc");
		com.set("pressure",0);
		com.set("temperature",0);
		com.set("humidity",0);
		com.set("createDate", new Date());
		com.set("deviceId", deviceId);
		if("0".equals(flag)){
			com.set("deviceState","通讯异常");
		}else{
			com.set("deviceState","关机");
		}
		com.save();
	}
	/**
	 * 从iot上获取空压机房数据插入到历史数据表中
	 * 
	 * @param lists
	 */
	public void handelKYJF(List<Devices> lists) {
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
					double temperature = jsonPoint.getDouble("l");
					// 湿度
					double humidity = jsonPoint.getDouble("k");
					// iot上更新日期+30分钟
					long at = jsonPoint.getDate("at").getTime() + 1000 * 60 * 5;
					// 当期日期
					long currentDate = new Date().getTime();
					// 如果当前时间还是大于最后一次在线时间加上5分钟，通讯异常or关机
					if (at < currentDate) {
						//再加上30分钟
						 at = at + 1000 * 60 *30;
						 //如果当前时间还是大于最后一次在线时间加上15分钟，关机
						 if (at < currentDate){
							// 关机
							commOrShutdwonKYJF(device,"1");
						 }else{
							// 通讯异常
							commOrShutdwonKYJF(device,"0");
						 }
					} else {
						int num = Integer.valueOf(arr[1]);

						String url2 = "https://iot.espressif.cn/v1/datastreams/data"
								+ num + "/datapoints/?row_count=1";
						JSONObject jsonobject2 = HttpUtils.doGet(url2, headers);
						// 压力
						double pressure = 0;
						if (jsonobject2.getString("status").equals("200")) {
							JSONArray jsonData2 = jsonobject2
									.getJSONArray("datapoints");
							if (jsonData2.size() > 0) {
								JSONObject jsonPoint2 = jsonData2
										.getJSONObject(0);
								pressure = jsonPoint2.getDouble("x");
							}
						}
						int deviceId = device.getInt("id");
						Compressor com = new Compressor();
						//压力
						double p = pressure / 10;
						//空压机房温度
						double t = temperature / 10;
						//过滤器温度
						double h = humidity / 10;
						com.set("pressure",p);
						com.set("temperature",t);
						com.set("humidity",h);
						boolean flag = true;
						// 压力
						if (p > 1800) {
							flag = false;
						}
						// 空压机房温度
						if (t > 42) {
							flag = false;
						}
						// //过滤器温度
						if (h > 70) {
							flag = false;
						}
						if(flag){
							com.set("deviceState","正常");
						}else{
							com.set("deviceState","异常");
						}
						com.set("createDate", new Date());
						com.set("deviceId", deviceId);
						com.save();
					}
				}
			}
		}
	}

	/**
	 * 仓储温湿度设备通讯异常处理
	 * 
	 * @param device
	 */
	public void commOrShutdownSHJ(Devices device,String flag) {
		// 设备id
		int deviceId = device.getInt("id");
		StorageHJ sr = new StorageHJ();
//		StorageHJ sr2 = StorageHJ.dao.findFirst("select * from storagehj order by createDate desc");
		sr.set("temperature", 0);
		sr.set("humidity", 0);
		if("1".equals(flag)){
			sr.set("state", "关机");
		}else{
			sr.set("state", "通讯异常");
		}
		sr.set("createDate", new Date());
		sr.set("deviceId", deviceId);
		sr.save();
	}

	/**
	 * 从iot获取实时仓储温湿度插入至本地历史记录表中
	 * 
	 * @param lists
	 */
	public void handlSHJ(List<Devices> lists) {
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
							 commOrShutdownSHJ(device,"1");
						 }else{
							 commOrShutdownSHJ(device,"0"); 
						 }
					} else {
						// 设备id
						int deviceId = device.getInt("id");
						StorageHJ sr = new StorageHJ();
						// 温度
						double temperature = jsonPoint.getDouble("l") / 10;
						sr.set("temperature", temperature);
						boolean flag = true;
						// 温度高于35异常
						if (temperature / 10 > 35) {
							flag = false;
						}
						// 湿度
						double humidity = jsonPoint.getDouble("k") / 10;
						sr.set("humidity", humidity);
						// 湿度高于75异常
						if (humidity / 10 > 75) {
							flag = false;
						}
						if (flag) {
							sr.set("state", "正常");
						} else {
							sr.set("state", "异常");
						}
						sr.set("createDate", new Date());
						sr.set("deviceId", deviceId);
						sr.save();
					}
				}
			}
		}
	}

	/**
	 * agv小车处理
	 * flag=0通讯异常，flag=1关机
	 * @param device
	 */
	public void commOrShutdownAgv(Devices device,String flag) {
		// 设备id
		int deviceId = device.getInt("id");
		AgvHistory agv = new AgvHistory();
//		AgvHistory agv2 = AgvHistory.dao.findFirst("select * from agvhistory order by createDate desc");
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
		agv.set("position", "0");
		agv.set("createDate", new Date());
		agv.set("deviceId", deviceId);
		agv.save();
	}

	/**
	 * 处理更新agv小车实时数据
	 * 
	 * @param lists
	 */
	public void handlAgv(List<Devices> lists) {
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
							 commOrShutdownAgv(device,"1");
						 }else{
							 //通讯异常
							 commOrShutdownAgv(device,"0");
						 }
					} else {
						// 设备id
						int deviceId = device.getInt("id");
						AgvHistory agv = new AgvHistory();
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
						agv.set("createDate", new Date());
						agv.set("deviceId", deviceId);
						agv.save();
					}
				}
			}
		}
	}
	/**
	 * flag=1关机=0通讯异常
	 * @param device
	 * @param flag
	 */
	public void commOrShutdownLsx(Devices device,String flag){
		// 设备id
		int deviceId = device.getInt("id");
		LsxHistory	lsxH = new LsxHistory();
//		LsxHistory lsxH2 = LsxHistory.dao.findFirst("select * from lsx_history order by createDate desc");
		if("1".equals(flag)){
			lsxH.set("pstate","shutdown");
			lsxH.set("pdepict","关机");	
		}else{
			lsxH.set("pstate","commerror");
			lsxH.set("pdepict","通讯异常");	
		}
		lsxH.set("pressure",0);
		lsxH.set("createDate",new Date());
		lsxH.set("deviceId", deviceId);
		lsxH.save();
	}
	
	/**
	 * 处理流水线压力数据
	 * @param lists
	 */
	public void handelLsx(List<Devices> lists){
		for (Devices device : lists) {
			// 数据模型编号
			String deviceNum = device.get("deviceNum");
			String[] arr = deviceNum.split("-");
			String url = "https://iot.espressif.cn/v1/datastreams/data" + arr[1]
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
						at = at + 1000 * 60 * 10;
						//如果还是小于最后在线时间加上15分钟说明关机
						if(at<currentDate){
							//关机
							commOrShutdownLsx(device,"1");
						}else{
							//通讯异常
							commOrShutdownLsx(device,"0");
						}
					}else{
						// 设备id
						int deviceId = device.getInt("id");
						LsxHistory lsxH  = new LsxHistory();
						//压力
						double press = jsonPoint.getInteger("x")/10;
						lsxH.set("pressure",press);
						if(press>550){
							lsxH.set("pstate","error");
							lsxH.set("pdepict","压力异常");	
						}else{
							lsxH.set("pstate","normal");
							lsxH.set("pdepict","正常");
						}
						lsxH.set("createDate",new Date());
						lsxH.set("deviceId", deviceId);
						lsxH.save();
					}
				}
			}
		}
	}
}
