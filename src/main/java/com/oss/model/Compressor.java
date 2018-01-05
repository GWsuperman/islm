package com.oss.model;


import com.jfinal.plugin.activerecord.Model;

/**
 * 空压机历史实体类
 * @author ZGW
 *
 */
public class Compressor extends Model<Compressor>{
	private static final long serialVersionUID = 1064291771401662738L;
	
	public static final Compressor dao = new Compressor().dao();

}
