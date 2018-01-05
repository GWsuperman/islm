package com.oss.service;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eova.common.utils.xx;
import com.jfinal.aop.Clear;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.Devices;
import com.oss.model.Liaisons;
import com.oss.model.MailInfo;
import com.utils.HttpUtils;
import com.utils.SendMailUtil;

/**
 * iot触发器接口
 * @author ZGW
 * @date 2017年12月7日 
 * @version 1.0 
 */
public class IotAPIService extends Controller{
	
	//dimension_index 可以选择 [0,1,2,3,4] 分别代表要监控的第几维数据。x,y,z,k,l
	// 设置请求头
	private static Map<String, String> headers = new HashMap<String, String>();

	{ 
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "token " + ownerKey + "");
	}
	private static String ownerKey = "e2f78f28a418e045b5a3b8c36cd385e415c92326";
	
	@Clear
	public void trigger(){
		//获取请求ip地址
		String addr = getRequest().getRemoteAddr();
		System.out.println("========================触发器请求："+addr);
		String did = getPara("did");
		if(did==null|| did.equals("")){
			System.out.println("+++++++++++++++++++++++++++++++请求异常!ip="+addr);
		}else{
			//获取设备信息
			Devices device = Devices.dao.findById(did);
			//是否已发送邮件 0未发送 1 准备发送 2已发送
			String isSend = device.getStr("isSend");
			if("0".equals(isSend)){
				device.set("isSend","1");
				device.update();
			}else if("1".equals(isSend)){
				String deviceType = device.getStr("deviceType");
				if("1".equals(deviceType)){
					handelKYJF(device);
				}else if("2".equals(deviceType)){
					handelSHJ(device);
				}else if("4".equals(deviceType)){
					handelLSX(device);
				}
			}
		}
		renderNull();
	}
	
	/**
	 * 发送邮件并修改发送状态
	 * @param device 设备信息
	 * @param str 邮件异常信息
	 *  @param abnormalState 异常状态
	 */
	public void handelDeviceEmail(Devices device,String str,String abnormalState){
		//获得有效的联络人清单
		List<Liaisons> liaisons = Liaisons.dao.searchLiaisons();
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
	 * 处理空压机房设备
	 */
	private void handelKYJF(Devices device){
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
				//温度
				double temperature = jsonPoint.getDouble("l");
				// 湿度
				double humidity = jsonPoint.getDouble("k");
				
				//获取压力值
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
				
				double p = pressure / 10;
				double t = temperature / 10;
				double h = humidity / 10;
				StringBuffer s = new StringBuffer();
				StringBuffer abnormalState = new StringBuffer();
				//获取触发器压力最大值和最小值
				Record rP = Db.findFirst("select * from devicetrigger where deviceType = 1 and attr=1");
				//获取机房温度最大值最小值
				Record jT = Db.findFirst("select * from devicetrigger where deviceType = 1 and attr=2");
				//获取过滤器最大值最小值
				Record gT = Db.findFirst("select * from devicetrigger where deviceType = 1 and attr=3");
				// 压力
				if (rP!=null && p > xx.toInt(rP.getStr("maxV"))) {
					s.append("压力异常，异常值："+p+"kpa，");
					abnormalState.append("压力异常、");
				} 
				// 空压机房温度
				if (jT!=null && t > xx.toInt(jT.getStr("maxV"))) {
					s.append("空压机房温度异常，异常值："+t+"℃，");
					abnormalState.append("机房温度异常、");
				}
				// //过滤器温度
				if (gT!=null && h > xx.toInt(gT.getStr("maxV"))) {
					s.append("过滤器温度异常，异常值："+h+"℃。");
					abnormalState.append("过滤器温度异常、");
				} 
				s.replace(s.length()-1,s.length(),"。");
				abnormalState.deleteCharAt(abnormalState.length()-1);
				handelDeviceEmail(device,s.toString(),abnormalState.toString());
			}
		}
	}
	
	/**
	 * 处理仓储温湿度设备
	 * @param device
	 */
	private void handelSHJ(Devices device){
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
				// 温度
				double temperature = jsonPoint.getDouble("l") / 10;
				// 湿度
				double humidity = jsonPoint.getDouble("k") / 10;
				//异常信息
				StringBuffer s = new StringBuffer();
				//异常状态
				StringBuffer abnormalState = new StringBuffer();
				
				//获取库房温度最大值和最小值
				Record t = Db.findFirst("select * from devicetrigger where deviceType = 2 and attr=4");
				//获取库房温度最大值最小值
				Record h = Db.findFirst("select * from devicetrigger where deviceType = 2 and attr=5");
				// 温度高于35异常
				if (t!=null && temperature > xx.toInt(t.getStr("maxV"))) {
					s.append("温度异常，异常值："+temperature+"℃，");
					abnormalState.append("温度异常、");
				}
				// 湿度高于75异常
				if (h!=null && humidity > xx.toInt(h.getStr("maxV"))) {
					s.append("湿度异常，异常值："+humidity+"%。");
					abnormalState.append("湿度异常、");
				}
				s.replace(s.length()-1,s.length(),"。");
				abnormalState.deleteCharAt(abnormalState.length()-1);
				handelDeviceEmail(device,s.toString(),abnormalState.toString());
			}
		}
	}
	
	/**
	 * 处理流水线压力监测设备
	 * @param device
	 */
	private void handelLSX(Devices device){
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
				//压力
				double press = jsonPoint.getInteger("x")/10;
				String s = "";
				String as = "";
				//获取流水线压力最大值最小值
				Record p = Db.findFirst("select * from devicetrigger where deviceType = 4 and attr=6");
				if(p!=null && press>xx.toDouble(p.get("maxV"))){
					s = "压力异常，异常值："+press+"kpa。";
					as = "压力异常";
				}
				handelDeviceEmail(device,s,as);
			}
		}
	}
}
