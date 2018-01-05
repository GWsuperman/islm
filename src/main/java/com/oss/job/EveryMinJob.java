package com.oss.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.quartz.JobExecutionContext;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oss.model.ComState;
import com.oss.model.Devices;
import com.oss.model.Liaisons;
import com.oss.model.LsxReal;
import com.oss.model.MailInfo;
import com.oss.model.StorageReal;
import com.utils.HttpUtils;
import com.utils.SendMailUtil;

/**
 * 每3分钟执行一次任务更新设备实时记录
 *
 * @date 2017年11月16日
 * @version V1.0
 */
public class EveryMinJob extends AbsJob {
	// 设置请求头
	private static Map<String, String> headers = new HashMap<String, String>();

	{ 
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "token " + ownerKey + "");
	}
	private static String ownerKey = "e2f78f28a418e045b5a3b8c36cd385e415c92326";
	//存储联络人清单
	private static List<Liaisons> liaisons = new ArrayList<Liaisons>();

	@Override
	protected void process(JobExecutionContext context) {
		// 获取设备类型为：1（空压机房）,2:仓储温湿度，3：agv小车的所有设备信息,4流水线压力检测
		List<Devices> lists1 = Devices.dao.findDevicesByType("1");
		List<Devices> lists2 = Devices.dao.findDevicesByType("2");
		List<Devices> lists4 = Devices.dao.findDevicesByType("4");
		liaisons.clear();
		//获得有效的联络人清单
		 liaisons = Liaisons.dao.searchLiaisons();
		// ping网关是否在线
		String url = "https://iot.espressif.cn/v1/device/ping/";
		JSONObject jsonObject = HttpUtils.doGet(url, headers);
		if (jsonObject.containsKey("is_online")) {
			// 设备在线状态true：在线，false：不在线
			boolean flag = jsonObject.getBoolean("is_online");
			if (flag) {
				// 处理空压机房数据
				handelKYJF(lists1);
				// 处理仓储温湿度数据
				handlSHJ(lists2);
				//流水线压力
				handelLsx(lists4);
			} else {
				// 最后一次在线时间
				long lastDate = jsonObject.getDate("last_active").getTime() + 1000 * 60 * 5;
				long newDate = new Date().getTime();
				// 如果当前时间还是大于最后一次在线时间加上五分钟，说明掉线了or关机
				if (lastDate < newDate) {
					lastDate = lastDate + 1000 * 60 * 10;
					//如果还是小于最后在线时间加上30分钟说明关机
					if(lastDate<newDate){
						//关机
						for (Devices device : lists1) {
							commOrShutdownKYJF(device,"1");
						}
						for (Devices device : lists2) {
							commOrShutdownSHJ(device,"1");
						}
						for(Devices device:lists4){
							commOrShutdownLsx(device, "1");
						}
					}else{
						//通讯异常
						for (Devices device : lists1) {
							commOrShutdownKYJF(device,"0");
						}
						for (Devices device : lists2) {
							commOrShutdownSHJ(device,"0");
						}
						for(Devices device:lists4){
							commOrShutdownLsx(device, "0");
						}
					}
				}
			}
		}

	}

	/**
	 * 发送邮件并修改发送状态
	 * @param device 设备信息
	 * @param str 邮件异常信息
	 *  @param abnormalState 异常状态
	 */
	public void handelDeviceEmail(Devices device,String str,String abnormalState){
		if(liaisons.size()>0){
			//设备名称
			String deviceName = device.getStr("deviceName");
			//设备区域
			String region = device.getStr("region");
			String content = "尊敬的用户你好，本地设备名称为"+deviceName+"的设备异常，异常信息为："+str+"所在区域："+region+"，请及时处理！";
			//存储多个联系人
			InternetAddress[] addresses = new InternetAddress[liaisons.size()];
			int i=0;
			try {
				for(Liaisons list:liaisons){
					//收件人姓名
					String name = list.getStr("name");
					//收件人email
					String email = list.getStr("email");
					addresses[i] = (new InternetAddress(email,name, "UTF-8"));
					i++;
					int lv = list.getInt("lv");
					MailInfo mailInfo = new MailInfo();
					mailInfo.set("addressee",name);
					mailInfo.set("content", content);
					mailInfo.set("createDate",new Date());
					mailInfo.set("abnormalState", abnormalState);
					mailInfo.set("lv",lv);
					mailInfo.set("deviceName",deviceName);
					mailInfo.save();
				}
				//向多人发送邮件
				SendMailUtil.sendMail(content,addresses);
				device.set("isSend","2");
				device.update();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 空压机房设备处理
	 * flag=0通讯异常，1关机
	 * @param device
	 */
	public void commOrShutdownKYJF(Devices device,String flag) {
		int deviceId = device.get("id");
		ComState comState = ComState.dao.getComStateDeviceId(deviceId);
		boolean isNull = true;
		if (comState != null) {
			isNull = false;
		} else {
			comState = new ComState();
		}
		if("0".equals(flag)){
			comState.set("pstate", "comerror");
			comState.set("pdepict", "通讯异常");
			comState.set("tstate", "comerror");
			comState.set("tdepict", "通讯异常");
			comState.set("hstate", "comerror");
			comState.set("hdepict", "通讯异常");
		}else{
			comState.set("pstate", "shutdown");
			comState.set("pdepict", "关机");
			comState.set("tstate", "shutdown");
			comState.set("tdepict", "关机");
			comState.set("hstate", "shutdown");
			comState.set("hdepict", "关机");
		}
		
		//空压机压力
		comState.set("pressure",0);
		//机房温度
		comState.set("temperature",0);
		//过滤器温度
		comState.set("humidity",0);
		comState.set("updataTime", new Date());
		if (isNull) {
			comState.set("deviceId", deviceId);
			comState.save();
		} else {
			comState.update();
		}
	}
	

	/**
	 * 从iot上获取空压机房数据更新本地数据库
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
					// iot上更新日期
					long at = jsonPoint.getDate("at").getTime() + 1000 * 60 * 5;
					// 当期日期
					long currentDate = new Date().getTime();
					// 如果当前时间还是大于最后一次在线时间加上五分钟，说明掉线了
					if (at < currentDate) {
						at = at + 1000 * 60 * 10;
						//如果还是小于最后在线时间加上30分钟说明关机
						if(at<currentDate){
							//关机
							commOrShutdownKYJF(device,"1");
						}else{
							// 通讯异常
							commOrShutdownKYJF(device,"0");
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
						ComState comState = ComState.dao
								.getComStateDeviceId(deviceId);
						boolean isNull = true;
						if (comState != null) {
							isNull = false;
						} else {
							comState = new ComState();
						}
						double p = pressure / 10;
						double t = temperature / 10;
						double h = humidity / 10;
						//空压机压力
						comState.set("pressure",p);
						//机房温度
						comState.set("temperature",t);
						//过滤器温度
						comState.set("humidity",h);
						boolean flag = true;
						StringBuffer s = new StringBuffer();
						StringBuffer abnormalState = new StringBuffer();
						// 压力
						if (p > 1800) {
							comState.set("pstate", "error");
							comState.set("pdepict", "压力异常");
							flag = false;
							s.append("压力异常，异常值："+p+"kpa，");
							abnormalState.append("压力异常、");
						} else {
							comState.set("pstate", "success");
							comState.set("pdepict", "压力正常");
						}
						// 空压机房温度
						if (t > 42) {
							comState.set("tstate", "error");
							comState.set("tdepict", "温度异常");
							s.append("空压机房温度异常，异常值："+t+"℃，");
							abnormalState.append("机房温度异常、");
							flag = false;
						} else {
							comState.set("tstate", "success");
							comState.set("tdepict", "温度正常");
						}
						// //过滤器温度
						if (h > 70) {
							comState.set("hstate", "error");
							comState.set("hdepict", "温度异常");
							s.append("过滤器温度异常，异常值："+h+"℃。");
							abnormalState.append("过滤器温度异常");
							flag = false;
						} else {
							if(!flag){
								s.replace(s.length()-1,s.length(),"。");
								abnormalState.deleteCharAt(abnormalState.length()-1);
							}
							comState.set("hstate", "success");
							comState.set("hdepict", "温度正常");
						}
						//通知发送状态0未通知，1准备通知，2已通知，当两次检测时通知状态为1，就发送邮件通知
						String isSend = device.getStr("isSend");
						if(flag){
							device.set("isSend","0");
							device.update();
						}else{
							if(isSend.equals("0")){
								device.set("isSend","1");
								device.update();
							}else if(isSend.equals("1")){
								handelDeviceEmail(device,s.toString(),abnormalState.toString());
							}
						}
						comState.set("updataTime", new Date());
						if (isNull) {
							comState.set("deviceId", deviceId);
							comState.save();
						} else {
							comState.update();
						}
					}
				}
			}
		}
	}

	/**
	 * 仓储温湿度设备处理
	 * 
	 * @param device
	 */
	public void commOrShutdownSHJ(Devices device,String flag) {
		// 设备id
		int deviceId = device.getInt("id");
		StorageReal sr = StorageReal.dao.getStorageByDeviceId(deviceId);
		boolean isNull = true;
		if (sr != null) {
			isNull = false;
		} else {
			sr = new StorageReal();
		}
		sr.set("temperature", 0);
		sr.set("humidity", 0);
		if("1".equals(flag)){
			sr.set("state", "关机");
		}else{
			sr.set("state", "通讯异常");
		}
		sr.set("updataTime", new Date());
		if (isNull) {
			sr.set("deviceId", deviceId);
			sr.save();
		} else {
			sr.update();
		}
	}

	/**
	 * 从iot获取实时仓储温湿度更新至本地数据库中
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
						commOrShutdownSHJ(device,"0");
					} else {
						// 设备id
						int deviceId = device.getInt("id");
						StorageReal sr = StorageReal.dao
								.getStorageByDeviceId(deviceId);
						boolean isNull = true;
						if (sr != null) {
							isNull = false;
						} else {
							sr = new StorageReal();
						}
						// 温度
						double temperature = jsonPoint.getDouble("l") / 10;
						sr.set("temperature", temperature);
						boolean flag = true;
						//异常信息
						StringBuffer s = new StringBuffer();
						//异常状态
						StringBuffer abnormalState = new StringBuffer();
						// 温度高于35异常
						if (temperature> 35) {
							s.append("温度异常，异常值："+temperature+"℃，");
							abnormalState.append("温度异常、");
							flag = false;
						}
						// 湿度
						double humidity = jsonPoint.getDouble("k") / 10;
						sr.set("humidity", humidity);
						// 湿度高于75异常
						if (humidity> 75) {
							s.append("湿度异常，异常值："+humidity+"%。");
							abnormalState.append("湿度异常");
							flag = false;
						}else{
							if(!flag){
								s.replace(s.length()-1,s.length(),"。");
								abnormalState.deleteCharAt(abnormalState.length()-1);
							}
						}
						if (flag) {
							sr.set("state", "正常");
						} else {
							sr.set("state", "异常");
						}
						//通知发送状态0未通知，1准备通知，2已通知，当两次检测时通知状态为1，就发送邮件通知
						String isSend = device.getStr("isSend");
						if(flag){
							device.set("isSend","0");
							device.update();
						}else{
							if(isSend.equals("0")){
								device.set("isSend","1");
								device.update();
							}else if(isSend.equals("1")){
								handelDeviceEmail(device,s.toString(),abnormalState.toString());
							}
						}
						sr.set("updataTime", new Date());
						if (isNull) {
							sr.set("deviceId", deviceId);
							sr.save();
						} else {
							sr.update();
						}
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
		LsxReal lsxReal = LsxReal.dao.findByDeviceId(deviceId);
		boolean isNull = true;
		if (lsxReal != null) {
			isNull = false;
		} else {
			lsxReal = new LsxReal();
		}
		if("1".equals(flag)){
			lsxReal.set("pstate","shutdown");
			lsxReal.set("pdepict","关机");	
		}else{
			lsxReal.set("pstate","commerror");
			lsxReal.set("pdepict","通讯异常");	
		}
		lsxReal.set("pressure",0);
		lsxReal.set("updateTime",new Date());
		if (isNull) {
			lsxReal.set("deviceId", deviceId);
			lsxReal.save();
		} else {
			lsxReal.update();
		}
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
						LsxReal lsxReal = LsxReal.dao.findByDeviceId(deviceId);
						boolean isNull = true;
						if (lsxReal != null) {
							isNull = false;
						} else {
							lsxReal = new LsxReal();
						}
						//压力
						double press = jsonPoint.getInteger("x")/10;
						lsxReal.set("pressure",press);
						boolean flag = true;
						//异常信息
						StringBuffer s = new StringBuffer();
						//异常状态
						StringBuffer as = new StringBuffer();
						if(press>850){
							lsxReal.set("pstate","error");
							lsxReal.set("pdepict","压力异常");	
							flag =false;
							s.append("压力异常，异常值："+press+"kpa。");
							as.append("压力异常");
						}else{
							lsxReal.set("pstate","normal");
							lsxReal.set("pdepict","正常");
						}
						//通知发送状态0未通知，1准备通知，2已通知，当两次检测时通知状态为1，就发送邮件通知
						String isSend = device.getStr("isSend");
						if(flag){
							device.set("isSend","0");
							device.update();
						}else{
							if(isSend.equals("0")){
								device.set("isSend","1");
								device.update();
							}else if(isSend.equals("1")){
								handelDeviceEmail(device,s.toString(),as.toString());
							}
						}
						lsxReal.set("updateTime",new Date());
						if (isNull) {
							lsxReal.set("deviceId", deviceId);
							lsxReal.save();
						} else {
							lsxReal.update();
						}
					}
				}
			}
		}
	}
	
}
