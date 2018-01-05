package com.oss.model;

import com.jfinal.plugin.activerecord.Model;

/**
 * 异常通知邮件信息
 * @author ZGW
 * @date 2017年11月13日 
 * @version 1.0 
 */
public class MailInfo extends Model<MailInfo>{

	private static final long serialVersionUID = 1L;
	
	public static final MailInfo dao = new MailInfo().dao();

}
