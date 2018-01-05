//异常触发器页面级联
$(document).ready(function(){
	var $dt= $('#devicetype');
    var $a= $('#attr');
    $a.mask();
    
    $dt.eovacombo({onChange: function (oldValue, newValue) {
    	 $a.eovacombo().setValue("");
        if (newValue == "") {
        	 $a.mask();
            return;
        }
        $a.unmask();
//        var url = '/widget/comboJson?exp=select id ID,name CN from area where lv = 2 and pid = ' + newValue;
        //$city.eovacombo({url : url}).reload();
        $a.eovacombo({exp : 'triggerCascade,' + newValue}).reload();
    }});
});