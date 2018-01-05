package com.oss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.eova.aop.AopContext;
import com.eova.common.render.XlsRender;
import com.eova.common.utils.db.DbUtil;
import com.eova.config.PageConst;
import com.eova.model.Menu;
import com.eova.model.MetaField;
import com.eova.model.MetaObject;
import com.eova.service.sm;
import com.eova.template.common.util.TemplateUtil;
import com.eova.widget.WidgetManager;
import com.eova.widget.grid.GridController;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.activerecord.Record;

public class MyGridController extends GridController{
	final Controller ctrl = this;
	/**
	 * 导出查询
	 * 
	 * @throws Exception
	 */
	public void export() throws Exception {
		String objectCode = getPara(0);
		String menuCode = getPara(1);
		String pValue = getPara("pValue");
		String pField = getPara("pField");
		int pageNumber = getParaToInt(PageConst.PAGENUM, 1);
		int pageSize = getParaToInt(PageConst.PAGESIZE, 100000);
		MetaObject object = sm.meta.getMeta(objectCode);
		Menu menu = Menu.dao.findByCode(menuCode);

		intercept = TemplateUtil.initIntercept(object.getBizIntercept());

		// 构建查询
		List<Object> parmList = new ArrayList<Object>();
		String sql = WidgetManager.buildQuery(ctrl, object, menu, intercept, parmList);
		//判断是否为空
		if(pValue!=null&&pField!=null){
			sql = sql +" and "+pField+"="+pValue;
		}
		// 转换SQL参数
		Object[] paras = new Object[parmList.size()];
		parmList.toArray(paras);
		List<Record> data = Db.use(object.getDs()).find("select *" + DbUtil.formatSql(sql)+" limit "+(pageNumber-1)*pageSize+","+pageSize, paras);

		List<MetaField> fields = object.getFields();
		// 查询后置任务
		if (intercept != null) {
			AopContext ac = new AopContext(ctrl, data);
			intercept.queryAfter(ac);
		}
		// 根据表达式将数据中的值翻译成汉字
		WidgetManager.convertValueByExp(this, fields, data);

		Iterator<MetaField> it = fields.iterator();  
		while (it.hasNext()) {
			MetaField f = it.next();
			if (!f.getBoolean("is_show")) {
				it.remove();
			}
		}
		
		render(new XlsRender(data, fields, object));
	}

}
