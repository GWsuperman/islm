package com.oss.model;

import java.util.List;

import com.jfinal.plugin.activerecord.Model;

/**
 * 联系人实体类
 * @author ZGW
 * @date 2017年11月13日 
 * @version 1.0 
 */
public class Liaisons extends Model<Liaisons>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final Liaisons dao = new Liaisons().dao();
	
	/**
	 * 筛选接收邮件的联络人
	 * @return
	 */
	public List<Liaisons> searchLiaisons(){
		String sql = "select * from liaisons_info where isRecipients=1";
		return this.findByCache("deviceCache","liaisonCache", sql);
	}
}
