$(document).ready(function(){
    var $dNum = $('#devicenum');
    var $dName = $('#devicename');
    $dName.eovacombo({onChange: function (oldValue, newValue) {
    	if (newValue == "") {
            return;
        }
        $dNum.eovacombo({exp : 'selectDevice,' + newValue}).reload();
    }});
});