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
import com.oss.model.AgvReal;
import com.oss.model.Devices;
import com.oss.model.Liaisons;
import com.oss.model.MailInfo;
import com.utils.HttpUtils;
import com.utils.SendMailUtil;

/**
 * 每天执行定时任务
 *
 * @author Jieven
 * @date 2014-7-7
 */
public class EveryDayJob extends AbsJob {
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
		List<Devices> lists3 = Devices.dao.findDevicesByType("3");
		liaisons.clear();
		//获得有效的联络人清单
		 liaisons = Liaisons.dao.searchLiaisons();
		 String url = "https://iot.espressif.cn/v1/device/ping/";
			JSONObject jsonObject = HttpUtils.doGet(url, headers);
			if (jsonObject.containsKey("is_online")) {
				// 设备在线状态true：在线，false：不在线
				boolean flag = jsonObject.getBoolean("is_online");
				if (flag) {
					// 处理agv小车数据
					handlAgv(lists3);
				//流水线压力
				} else {
					// 最后一次在线时间
					long lastDate = jsonObject.getDate("last_active").getTime() + 1000 * 60 * 5;
					long newDate = new Date().getTime();
					// 如果当前时间还是大于最后一次在线时间加上五分钟，说明掉线了or关机
					if (lastDate < newDate) {
						lastDate = lastDate + 1000 * 60 * 10;
						//如果还是小于最后在线时间加上30分钟说明关机
						if(lastDate<newDate){
							for (Devices device : lists3) {
								commOrShutdownAgv(device,"1");
							}
						}else{
							for (Devices device : lists3) {
								commOrShutdownAgv(device,"0");
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
	 * agv小车处理
	 * 
	 * @param device
	 * @param flag 1关机，0通讯异常
	 */
	public void commOrShutdownAgv(Devices device,String flag) {
		// 设备id
		int deviceId = device.getInt("id");
		AgvReal agv = AgvReal.dao.getAgvRealByDeviceId(deviceId);
		boolean isNull = true;
		if (agv != null) {
			isNull = false;
		} else {
			agv = new AgvReal();
		}
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
		agv.set("updataTime", new Date());
		if (isNull) {
			agv.set("deviceId", deviceId);
			agv.save();
		} else {
			agv.update();
		}
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
						at = at + 1000 * 60 * 10;
						//如果还是小于最后在线时间加上15分钟说明关机
						if(at<currentDate){
							//关机
							commOrShutdownAgv(device,"1");
						}else{
							//通讯异常
							commOrShutdownAgv(device,"0");
						}
						
					} else {
						// 设备id
						int deviceId = device.getInt("id");
						AgvReal agv = AgvReal.dao
								.getAgvRealByDeviceId(deviceId);
						boolean isNull = true;
						if (agv != null) {
							isNull = false;
						} else {
							agv = new AgvReal();
						}
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
						// 位置
						String position = jsonPoint.getInteger("z").toString();
						boolean flag = true;
						//异常信息
						StringBuffer s = new StringBuffer();
						//异常状态
						StringBuffer as = new StringBuffer();
						// 异常状态
						int abnormalState = jsonPoint.getInteger("y");
						if (abnormalState == 1) {
							agv.set("abnormalState", "normal");
							agv.set("adepict", "正常");
						} else {
							agv.set("abnormalState", "error");
							agv.set("adepict", "异常");
							flag = false;
							s.append("运行异常，故障位置："+position+"。");
							as.append("运行异常");
						}
						//通知发送状态0未通知，2已通知， 判断是否已经发送过邮件
						String isSend = device.getStr("isSend");
						if(!flag){
							if(isSend.equals("0")){
								handelDeviceEmail(device,s.toString(),as.toString());
							}
						}else{
							device.set("isSend","0");
							device.update();
						}
						agv.set("position", position);
						agv.set("updataTime", new Date());
						if (isNull) {
							agv.set("deviceId", deviceId);
							agv.save();
						} else {
							agv.update();
						}
					}
				}
			}
		}
	}


}
