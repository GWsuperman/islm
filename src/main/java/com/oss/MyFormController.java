package com.oss;

import com.eova.aop.AopContext;
import com.eova.model.MetaObject;
import com.eova.model.User;
import com.eova.service.sm;
import com.eova.template.common.util.TemplateUtil;
import com.eova.widget.form.FormController;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;

public class MyFormController extends FormController{
	final Controller ctrl = this;
	
	public void detail(){
		AopContext ac = new AopContext(ctrl);
		buildFormData(false, ac);
		// 业务拦截
		try {
			intercept = TemplateUtil.initIntercept(ac.object.getBizIntercept());
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (intercept != null) {
			try {
				intercept.updateInit(ac);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		render("/eova/widget/form/detail.html");
	}

	/**
	 * 构建对象数据
	 */
	private MetaObject buildFormData(boolean isEdit, AopContext ac) {
		String objectCode = this.getPara(0);
		// 获取主键的值
		Object pkValue = getPara(1);

		MetaObject object = sm.meta.getMeta(objectCode);

		// 根据主键获取对象
		Record record = Db.use(object.getDs()).findById(object.getView(), object.getPk(), pkValue);
		setAttr("record", record);
		setAttr("object", object);

		ac.object = object;
		ac.record = record;

		return object;
	}
}
