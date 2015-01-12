function check() {
	
	//檢查是否輸入username
	var username = $("#name1").val();
	if(username==""){
		alert("請輸入姓名");
		return;
	}
	
	//檢查是否輸入userId
	var userId = $("#userId1").val();
	if(userId=="") {
		alert("請輸入身分證字號");
		return;
	}
	
	if(!checkID(userId)) {
		alert("請輸入正確的身分證字號");
		return;
	}
	
	//檢查是否輸入phone
	var phone1 = $("#phone1").val();
	if(phone1==""){
		alert("請輸入電話號碼");
		return;
	}
	
	var birth = $("#birth1").val();
	if(phone1==""){
		alert("請輸入身分證字號");
		return;
	}
	
	var oaid = $("#oaid").val();
	var member_id = $("#member_id").val(); 
	jQuery.ajax({
        type: 'GET',
        url: "/oa/checkApply",                
        data: {
        	oaid:oaid,
        	userId:userId,
        	attendant_id:member_id,
        	type: "child"
     	},
        success: function(data) {
	    	if(data=='ok') {
	    		alert("報名成功");
	    		applyfm1.submit();
	    	}
	    	else if(data=='no') {
	    		alert("您已經報名過這場活動了喔。");
	    	}
        },
        error: function(a,b,c) {
            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
        }
    });
	
}

function checkParentApply() {
	
	var ok = true;
	
	var username = $("#name2").val();
	var userId = $("#userId2").val();
	var phone = $("#phone2").val();
	var birth = $("#birth2").val();
	 
	if(username=="") {
		alert("請輸入姓名");
		return;
	}
	
	if(userId=="") {
		alert("請輸入身分證字號");
		return;
	}
	
	if(phone=="") {
		alert("請輸入電話號碼");
		return;
	}
	
	if(birth=="") {
		alert("請輸入出生年月日");
		return;
	}
	
	var oaid = $("#oaid").val();
	var member_id = $("#member_id").val(); // 登入報名者id	
	var child = document.getElementById('children');
	var selectedIndex = child.selectedIndex;
	var child_id = child[selectedIndex].id;
	jQuery.ajax({
        type: 'GET',
        url: "/oa/checkApply",
        data: {
        	oaid:oaid,
        	userId:userId,
        	attendant_id:child_id,
        	type: "parent"
     	},
        success: function(data) {
	    	if(data=='ok') {
	    		alert("報名成功");
	    		applyfm2.submit();
	    	}
	    	else if(data=='no') {
	    		alert("您已經報名過這場活動了喔。");
	    	}
        },
        error: function(a,b,c) {
            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
        }
    });
	
}

function atmCheck() {	
	var ok = true;
	var atm = $("#atm").val();
	
	if(atm.length!=5) {
		ok = false;
		alert("請輸入帳號後五碼");
		return;
	}
	else if(isNaN(atm)) {
		ok = false;
		alert("請輸入數字");
		return;
	}
	
	if(ok) {
		alert("送出成功");
		atmForm.submit();
	}
	
}

/* for option change event */
window.onload = function() {
	initApply();
	if(child!=null) {
	    child.onchange = function() {
	    	
	    	userId2.value = "";
	    	phone2.value = "";
	    	
	        var attendant_Input = document.getElementById('attendant_id');
	    	var selectedIndex = child.selectedIndex;
	    	var child_id = child[selectedIndex].id;
	    	if(child.selectedIndex  != 0) {
	    		username2.value = child.value;
	    		username2.readOnly = true;
	    		attendant_Input.value = child_id;
	    		
	    		var birth = document.getElementById('birth_' + child_id).value;
	    		if(birth!="") {
	    			birth2.value = birth;
	    			birth2.readOnly = true;
	    		} else {
	    			birth2.value = "";
	    			birth2.readOnly = false;
	    		}
	    		
	    	}
	    	else {
	    		username2.readOnly = false;
	    		username2.value = "";
	    		birth2.value = "";
    			birth2.readOnly = false;
	    	}
	    }
	}
}

var child, username1, username2, userId1, userId2, phone1, phone2, birth1, birth2;

function initApply() {
	
	child = document.getElementById('children');
	username1 = document.getElementById('name1');
	username2 = document.getElementById('name2');
	userId1 = document.getElementById('userId1');
	userId2 = document.getElementById('userId2');
	phone1 = document.getElementById('phone1');
	phone2 = document.getElementById('phone2');
	birth1 = document.getElementById('birth1');
	birth2 = document.getElementById('birth2');
	
	if(username1!=null) username1.value = "";
	if(username2!=null) { 
		username2.value = "";
		username2.readOnly = false;
	}
	if(userId1!=null) userId1.value = "";
	if(userId2!=null) userId2.value = "";
	if(phone1!=null) phone1.value = "";
	if(phone2!=null) phone2.value = "";
	if(birth1!=null) birth1.value = "";
	if(birth2!=null) birth2.value = "";
	
}
   
   
function checkID(id) {
  tab = "ABCDEFGHJKLMNPQRSTUVXYWZIO"                     
  A1 = new Array (1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,3,3,3,3,3,3 );
  A2 = new Array (0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5 );
  Mx = new Array (9,8,7,6,5,4,3,2,1,1);

  if ( id.length != 10 ) return false;
  i = tab.indexOf( id.charAt(0) );
  if ( i == -1 ) return false;
  sum = A1[i] + A2[i]*9;

  for ( i=1; i<10; i++ ) {
    v = parseInt( id.charAt(i) );
    if ( isNaN(v) ) return false;
    sum = sum + v * Mx[i];
  }
  if ( sum % 10 != 0 ) return false;
  return true;
}



