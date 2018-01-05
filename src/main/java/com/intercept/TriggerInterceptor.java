package com.intercept;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eova.aop.AopContext;
import com.eova.aop.MetaObjectIntercept;
import com.eova.common.Easy;
import com.eova.common.utils.xx;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import com.oss.model.Devices;
import com.utils.HttpUtils;

/**
 * 异常触发器页面拦截器
 * @author ZGW
 * @date 2017年12月6日 
 * @version 1.0 
 */
public class TriggerInterceptor extends MetaObjectIntercept{
	
	/**
	 * iot上设备id
	 */
	private static final  String DEVICE_ID = "4922";
	
	/**
	 * iot用户token
	 */
	private static final String USER_TOKEN = "c21fcc616051b018615e82c273fd07898300c8dc";
	
	// 设置请求头
	private static Map<String, String> headers = new HashMap<String, String>();

	{ 
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "token " + USER_TOKEN + "");
	}
    /**
	 * 新增前置任务(事务内)
	 * </pre>
	 */
    public String addBefore(AopContext ac) throws Exception {
    	//设备类型
    	String deviceType = ac.record.get("devicetype");
    	//设备属性id
    	String attrId = ac.record.get("attr");
    	Record record = Db.findFirst("select * from deviceattr where id = ?",attrId);
    	String attr = record.getStr("num");
    	//通过设备类型和属性值查询触发器数据
    	int num = Db.queryInt("select count(*) from devicetrigger where deviceType=? and attr=?",deviceType,attrId);
    	if(num!=0){
    		return Easy.error("已有该触发器!");
    	}
    	//最大值
    	String maxv = ac.record.getStr("maxv");
    	//最小值
    	String minv = ac.record.getStr("minv");
    	boolean mxFlag = false;
    	boolean mnFlag = false;
    	if(maxv==null || maxv.equals("") ){
    		mxFlag = true;
    	}
    	if(minv==null || minv.equals("")){
    		mnFlag = true;
    	}
    	if(mxFlag && mnFlag){
    		return Easy.error("最大值和最小值必须填一项!");
    	}
    	//触发器名称
    	String name = ac.record.getStr("name");
    	//json post提交参数
    	JSONObject params = new JSONObject();
    	//触发器数组
    	JSONArray ja = new JSONArray();
    	List<Devices> lists = Devices.dao.findDevicesByType(deviceType);
    		for(Devices list:lists){
    			//设备编号
        		String deviceNum = list.getStr("deviceNum");
        		String[] strs = deviceNum.split("-");
        		//数据模型编号
        		String streamName = null;
        		if(attr.equals("0")){
        			streamName = "data"+strs[1];
        		}else{
        			streamName = strs[0];
        		}
        		JSONObject para = new JSONObject();
        		//iot设备id
        		para.put("device_id", DEVICE_ID);
        		//模型编号
        		para.put("stream_name",streamName);
        		//触发器名称
        		para.put("name",name+list.getInt("id"));
        		//维度
        		para.put("dimension_index",attr);
        		//时间间隔
        		para.put("interval","0");
        		//运算符avg 平均值
        		para.put("interval_func","4");
        		//url
        		para.put("url","http://116.62.135.105/iotTriService/trigger?did="+list.getInt("id"));
        		//最大值为空
        		if(mxFlag){
        			//比较条件 <=
            		para.put("compare_type","6");
            		//临界值最小值
            		para.put("threshold_value",xx.toDouble(minv)*10);
        		}else{
        			//最小值为空的话
        			if(mnFlag){
        				//比较条件 >=
                		para.put("compare_type","4");
                		//临界值最大值
                		para.put("threshold_value",xx.toDouble(maxv)*10);
        			}else{
        				//比较条件 <=
                		para.put("compare_type","6");
                		//临界值最小值
                		para.put("threshold_value",xx.toDouble(minv)*10);
                		
                		JSONObject para2 = new JSONObject();
                		//比较条件 >=
                		para2.put("compare_type","4");
                		//临界值最大值
                		para2.put("threshold_value",xx.toDouble(maxv)*10);
                		//iot设备id
                		para2.put("device_id", DEVICE_ID);
                		//模型编号
                		para2.put("stream_name",streamName);
                		//触发器名称
                		para2.put("name",name+list.getInt("id"));
                		//维度
                		para2.put("dimension_index",attr);
                		//时间间隔
                		para2.put("interval","0");
                		//运算符avg 平均值
                		para2.put("interval_func","4");
                		//url
                		para2.put("url","http://116.62.135.105/iotTriService/trigger?did="+list.getInt("id"));
                		//添加进数组中
                		ja.add(para2);
        			}
        		}
        		//添加进数组中
        		ja.add(para);
        	}
    	params.put("triggers", ja);
    	StringBuffer sb = new StringBuffer();
    	JSONObject jsonObject = HttpUtils.doPost("https://iot.espressif.cn/v1/triggers/", headers,null, params.toJSONString());
    	boolean flag = false;
    	//验证是否添加成功并获取triggerId保存至数据库中
    	if(jsonObject.containsKey("status")){
    		if("200".equals(jsonObject.getString("status"))){
    			JSONArray array = jsonObject.getJSONArray("triggers");
    			for(int i=0;i<array.size();i++){
    				JSONObject trigger = array.getJSONObject(i);
    				String tId = trigger.getString("id");
    				sb.append(tId+"-");
    			}
    			flag = true;
    		}
    	}
    	if(!flag){
    		//iot上添加触发器失败
    		return Easy.info("添加失败!");
    	}
    	sb.deleteCharAt(sb.length()-1);
    	ac.record.set("iottrigerid",sb.toString());
    	ac.record.set("createdate",new Date());
        return null;
    }
    /**
     * 删除前置任务(事务内)
     * <pre>
     * ac.record    当前删除对象数据
     * -------------
     * 用法：删除前置检查
     * </pre>
     */
    public String deleteBefore(AopContext ac) throws Exception {
    	//触发器id
    	String iotTriggerId = ac.record.get("iottrigerid");
    	if(iotTriggerId==null){
    		return Easy.error("删除失败!");
    	}
    	//json post提交参数
    	JSONObject params = new JSONObject();
    	//触发器数组
    	JSONArray ja = new JSONArray();
    	String[] strs = iotTriggerId.split("-");
    	for(int i=0;i<strs.length;i++){
    		JSONObject jo = new JSONObject();
    		jo.put("id",strs[i]);
    		ja.add(jo);
    	}
    	params.put("triggers", ja);
    	JSONObject jsonObject = HttpUtils.doPost("https://iot.espressif.cn/v1/triggers/?method=DELETE", headers,null, params.toJSONString());
    	if(!"200".equals(jsonObject.getString("status"))){
    		System.out.println("+++++++++删除失败，触发器名称："+ac.record.getStr("name"));
//    		return Easy.error("删除失败!");
    	
    	}
    	return null;
    }
    
    /**
     * 更新前置任务(事务内)
     * 先删除iot上触发器再创建
     * </pre>
     */
    public String updateBefore(AopContext ac) throws Exception {
       	//最大值
    	String maxv = ac.record.getStr("maxv");
    	//最小值
    	String minv = ac.record.getStr("minv");
    	boolean mxFlag = false;
    	boolean mnFlag = false;
    	if(maxv==null || maxv.equals("") ){
    		mxFlag = true;
    	}
    	if(minv==null || minv.equals("")){
    		mnFlag = true;
    	}
    	if(mxFlag && mnFlag){
    		return Easy.error("最大值和最小值必须填一项!");
    	}
    	
    	//触发器id
    	String iotTriggerId = ac.record.get("iottrigerid");
    	//json post提交参数
    	JSONObject deleParams = new JSONObject();
    	//触发器数组
    	JSONArray jArray = new JSONArray();
    	String[] strArr = iotTriggerId.split("-");
    	for(int i=0;i<strArr.length;i++){
    		JSONObject jo = new JSONObject();
    		jo.put("id",strArr[i]);
    		jArray.add(jo);
    	}
    	deleParams.put("triggers", jArray);
    	//删除触发器
    	JSONObject deleObject = HttpUtils.doPost("https://iot.espressif.cn/v1/triggers/?method=DELETE", headers,null, deleParams.toJSONString());
    	if(!"200".equals(deleObject.getString("status"))){
    		System.out.println("+++++++++删除失败，触发器名称："+ac.record.getStr("name"));
//    		return Easy.error("更新失败!");
    	}
    	//新增
    	//设备类型
    	String deviceType = ac.record.get("devicetype");
    	//设备属性id
    	String attrId = ac.record.get("attr");
    	Record record = Db.findFirst("select * from deviceattr where id = ?",attrId);
    	String attr = record.getStr("num");
    	
    	//触发器名称
    	String name = ac.record.getStr("name");
    	//json post提交参数
    	JSONObject params = new JSONObject();
    	//触发器数组
    	JSONArray ja = new JSONArray();
    	List<Devices> lists = Devices.dao.findDevicesByType(deviceType);
    		for(Devices list:lists){
    			//设备编号
        		String deviceNum = list.getStr("deviceNum");
        		String[] strs = deviceNum.split("-");
        		//数据模型编号
        		String streamName = null;
        		if(attr.equals("0")){
        			streamName = "data"+strs[1];
        		}else{
        			streamName = strs[0];
        		}
        		JSONObject para = new JSONObject();
        		//iot设备id
        		para.put("device_id", DEVICE_ID);
        		//模型编号
        		para.put("stream_name",streamName);
        		//触发器名称
        		para.put("name",name+list.getInt("id"));
        		//维度
        		para.put("dimension_index",attr);
        		//时间间隔
        		para.put("interval","0");
        		//运算符avg 平均值
        		para.put("interval_func","4");
        		//url
        		para.put("url","http://116.62.135.105/iotTriService/trigger?did="+list.getInt("id"));
        		//最大值为空
        		if(mxFlag){
        			//比较条件 <=
            		para.put("compare_type","6");
            		//临界值最小值
            		para.put("threshold_value",xx.toDouble(minv)*10);
        		}else{
        			//最小值为空的话
        			if(mnFlag){
        				//比较条件 >=
                		para.put("compare_type","4");
                		//临界值最大值
                		para.put("threshold_value",xx.toDouble(maxv)*10);
        			}else{
        				//比较条件 <=
                		para.put("compare_type","6");
                		//临界值最小值
                		para.put("threshold_value",xx.toDouble(minv)*10);
                		
                		JSONObject para2 = new JSONObject();
                		//比较条件 >=
                		para2.put("compare_type","4");
                		//临界值最大值
                		para2.put("threshold_value",xx.toDouble(maxv)*10);
                		//iot设备id
                		para2.put("device_id", DEVICE_ID);
                		//模型编号
                		para2.put("stream_name",streamName);
                		//触发器名称
                		para2.put("name",name+list.getInt("id"));
                		//维度
                		para2.put("dimension_index",attr);
                		//时间间隔
                		para2.put("interval","0");
                		//运算符avg 平均值
                		para2.put("interval_func","4");
                		//url
                		para2.put("url","http://116.62.135.105/iotTriService/trigger?did="+list.getInt("id"));
                		//添加进数组中
                		ja.add(para2);
        			}
        		}
        		//添加进数组中
        		ja.add(para);
        	}
    	params.put("triggers", ja);
    	StringBuffer sb = new StringBuffer();
    	JSONObject jsonObject = HttpUtils.doPost("https://iot.espressif.cn/v1/triggers/", headers,null, params.toJSONString());
    	boolean flag = false;
    	//验证是否添加成功并获取triggerId保存至数据库中
    	if(jsonObject.containsKey("status")){
    		if("200".equals(jsonObject.getString("status"))){
    			JSONArray array = jsonObject.getJSONArray("triggers");
    			for(int i=0;i<array.size();i++){
    				JSONObject trigger = array.getJSONObject(i);
    				String tId = trigger.getString("id");
    				sb.append(tId+"-");
    			}
    			flag = true;
    		}
    	}
    	if(!flag){
    		//iot上添加触发器失败
    		return Easy.info("更新失败!");
    	}
    	sb.deleteCharAt(sb.length()-1);
    	ac.record.set("iottrigerid",sb.toString());
      	ac.record.set("updateDate",new Date());
        return null;
    }
}
