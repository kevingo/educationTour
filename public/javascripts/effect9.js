var url1 = "/ajax/getMemberListByMemberGroup";
var url2 = "/ajax/getMemberListByKeyword";
var group_ID = 0;
var selectedParentsID=",";
var selectedSearchMembersID=","
var selectedGroupMembersID=","
var element_limit = 3;
	
	
$(function(){
	
	
	
	//change tab
	var tab_name = window.location.href.split("#");
	if (tab_name.length>1) { 
		if(tab_name[1]=="")
			tab_name=setup;
		$("#setup").removeClass("active");
		$("#"+tab_name[1]).addClass("active");
		$("div.control_content li.active").removeClass("active");
		$("a[href='#"+tab_name[1]+"']").parents("li").addClass("active");
		
		$("#gotop").trigger("click");
		//$("a[href='#"+tab_name[1]+"']").click();
	}
	
	$("#add_elements").on("click",function(event){
		var obj = $(this).parent();
		if ($(obj).find("div.element").length<element_limit) { $("#element_pop").modal("toggle"); }
	});
	
	//#add_element pop - new button
	$("#add_element li").on("click",function(event){
		$("#add_element li").removeClass("active");
		$(this).addClass("active");
		if ($(this).hasClass("addeleli01")) {
			$("#add_element div.main,#add_element div.modal-footer a:not(.ref)").hide();
			$("#add_element div.ref,#add_element div.modal-footer a.ref").show();
			$("div.ref .lessons").html("");
		}
	});
	
	//elements(tags) pop
	$("#element_pop li").on("click",function(event){
		$("#add_elements").before("<div class='element'>"+$(this).html()+"<span>x</span><input type='hidden' name='element[]' value='"+$(this).attr("eid")+"'></div>");
		$("div.element span").off("click").on("click",function(event){ $(this).parent().remove(); event.stopPropagation(); });
		$("#element_pop").modal("hide");
	});
	
	//element (tags) X (delete)
	$("div.element span").off("click").on("click",function(event){ $(this).parent().remove(); event.stopPropagation(); });
	
	//bank popup
	$("#bankList").on("click",function(event){
		$.get("/public/javascripts/data/bank.txt",function(data){
			eval(data);
			var ul = "";
			for(var i=0;i<bank.length;i++) { ul += "<li bid='"+bank[i][0]+"' bname='"+bank[i][1]+"'><span>("+bank[i][0]+")</span>"+bank[i][1]+"</li>"; }
			$("#banks .bank_list").html("<ul>"+ul+"</ul>");
			$("#banks .bank_list li").off("click").on("click",function(event){
				//$("#bank_id").val($(this).attr("bid"));
				$("#bank_name").val($(this).attr("bname")+"("+$(this).attr("bid")+")");
				$("#banks").modal("toggle");
			});
			$("#banks").modal("toggle");
		});
	});
	
	//select all for relation Member
	$("thead.relation :checkbox").on("click",function(event){
		selectedParentsID=",";
		if ($(this).is(":checked")) {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", true);
			$(this).parents("table.table").find("tbody :checkbox").each(function(){
				selectedParentsID+=$(this).val()+",";
			});
		}
		else {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", false);
		}
	});
	
	//select all for seach member list
	$("thead.searchMember :checkbox").on("click",function(event){
		selectedSearchMembersID=",";
		if ($(this).is(":checked")) {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", true);
			$(this).parents("table.table").find("tbody :checkbox").each(function(){
				selectedSearchMembersID+=$(this).val()+",";
				
			});
		}
		else {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", false);
		}
		//alert(selectedSearchMembersID);
	});
	
	//select all for group member list
	$("thead.groupMember :checkbox").on("click",function(event){
		selectedGroupMembersID=",";
		if ($(this).is(":checked")) {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", true);
			$(this).parents("table.table").find("tbody :checkbox").each(function(){
				selectedGroupMembersID+=$(this).val()+",";
				
			});
		}
		else {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", false);
		}
	});
	
	//單選 關係  ok
	$("tbody.relation :checkbox").on("click",function(event){
		if ($(this).is(":checked")) {
			var id = $(this).val()+","
			selectedParentsID +=id;
		}
		else {
			var id = ","+$(this).val()+","
			selectedParentsID=selectedParentsID.replace(id, ",");
		}		
		//alert(selectedParentsID);
	});
				
	//chose school
	$("button.chose_school").off("click").on("click",function(event){
		$("div.page").hide();
		school_dialog();
		$("div.city").show();
		$('#school').modal("toggle");
	});
	
	//class group

	//new invite
	$("#newinvite button.search").on("click",function(event){ invite_search(); event.stopPropagation(); });
	
	//member in group list
	$("div.class_list li a").on("click",function(event){
		group_ID = $(this).attr("group_id");
		$("#sendMail").attr("href","/memberGroup/sendMail/"+group_ID);
		$("#groupId").val(group_ID);
		getMemberInGroup();
		event.stopPropagation();
	});
	$("div.class_list li a:first").trigger("click");
	
	//批次匯入
	$("#csv .btn-info").on("click",function(event){
		if (($("#picupload label.tip").length<1)&&(!/^$/i.test($("#picfile").val()))) {
			if(group_ID>0)
				$("#relationForm").submit();
			else
				alert("請先點選欲匯入之班群");
		}
		else{
			alert("請選擇正確的檔案。");
		}
	});
	$("#picfile").on("change",function(event){
		$("#picupload label.tip").remove();
		if (/\.(csv)$/i.test($(this).val())) {
			var file_name = $(this).val().split(/(\\|\/)/);
			$("#subfile").val(file_name[file_name.length-1]);
		}
		else {
			$(this).val("");
			$("#subfile").val("");
			$(this).after("<label class='tip'>僅支援csv格式</label>");
		}
	});
	
	//tab-1 save (basic info)
	$("#btn1").on("click",function(event){
		event.preventDefault();
		$("#setup span.check").remove();
		$("#setup input.require").each(function(i){ check_format($(this)); });
		if ($("#setup span.checkno").length<1) { $("#setupFM").submit(); }
	});
	
	//tab-2 save (password)
	$("#btn2").on("click",function(event){
		event.preventDefault();
		$("#password span.check").remove();
		$("#password input.require").each(function(i){ check_format($(this)); });
		if ($("#password span.checkno").length<1) {
			var oldpwd = $('#oldPassword').val();
			var pwd = $('#newPassword').val();
			var repwd = $('#renewPassword').val();
			if(pwd==repwd){
				jQuery.ajax({
					  url: '/member/updatePassword',
					  type: 'POST',
					  async: false,
					  data: {
					  	oldPassword:oldpwd,
					  	newPassword:pwd
					  },
					  success: function(response) {
						  alert(response);		  
					  },
					  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
					});
			} 
			else
				alert("請確認兩次新密碼輸入為相同的密碼。");
		}
	});
	
	//tab-7 save (bank info)
	$("#btn3").on("click",function(event){
		event.preventDefault();
		$("#account span.check").remove();
		$("#account input.require").each(function(i){ check_format($(this)); });
		if ($("#account span.checkno").length<1) { 
			var code = $('#bank_name').val();
			var account = $('#accountATM').val();
			jQuery.ajax({
				  url: '/member/updateBankAccount',
				  type: 'POST',
				  async: false,
				  data: {
				  	bankCode:code,
				  	atmAccount:account
				  },
				  success: function(response) {
					  alert(response);	
					  reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});
		}
		else
			alert("請確認銀行代碼與帳號皆已填寫。");
	});

});

function getMemberInGroup() {
	$("#member_list").html("<tr><td colspan='"+$("#member_list").parent().find("thead td").length+"'>Loading...</td></tr>");
	$.post(url1,{group_id:group_ID},function(json){ Member_list(json); },"json");
	/*Member_list({
"response":{
"id":1,
"result":"true",
"count":2,
"name":"測試班群1",
"members":[
{
"id":7,
"schoolName":null,
"status":"已經加入",
"email":"judy@cacet.org",
"name":"judy",
"className":null
},
{
"id":8,
"schoolName":null,
"status":"已經加入",
"email":"linda@cacet.org",
"name":"linda",
"className":null
},
{
"id":4,
"schoolName":null,
"status":"尚未審核通過",
"email":"kevingo75@gmail.com",
"name":"kevingo75",
"className":null
},
{
"id":16,
"schoolName":null,
"status":"尚未被接受",
"email":"student@gmail.com",
"name":"student",
"className":null
}
]
}
});*/
}

function Member_list(json_data) {
	var data = json_data.response;
	if (/true/i.test(data.result)) {
		var trs = "";
		if(data.name!=null){
			$("#groupName").val(data.name);
			$("span.classtitle").html(" "+data.name);
		}
		else{
			$("#groupName").val(null);
			$("span.classtitle").html("");
		}
		
		if(data.members.length==0){
			
			$("#member_list").html("<tr><td colspan='"+$("#member_list").parent().find("thead td").length+"'>無資料</td></tr>");
		}
		else{
			for (var i=0;i<data.members.length;i++) {
				trs += '<tr><td class="mc_td1"><input id=groupMember_'+data.members[i].id+' type="checkbox" onclick=selectedGroupMember('+ data.members[i].id +') value='+data.members[i].id+'></td>';
				trs += '<td class="mc_td2">'+data.members[i].name+'</td>';
				trs += '<td class="mc_td3">'+data.members[i].email+'</td>';
				trs += '<td class="mc_td4">'+data.members[i].groupName+'</td>';
				trs += '<td class="mc_td5">'+data.members[i].schoolName+'</td>';
				trs += '<td class="mc_td6">'+data.members[i].className+'</td>';
				trs += '<td class="mc_td7">'+((data.members[i].status=="尚未審核通過")?"<a class='btn btn-mini btn-success' onClick='AcceptGroupInvite("+data.members[i].id+",\""+data.members[i].groupName+"\")'>接受申請加入</a>":data.members[i].status)+'</td></tr>';
			}
			$("#member_list").html(trs);
		}
	}
	else { $("#member_list").html("<tr><td colspan='"+$("#member_list").parent().find("thead td").length+"'>"+data.reason+"</td></tr>"); }
}

function selectedSearchMember(id) {
	
	if ($("#member_" + id).is(":checked")) {
		var id = $("#member_" + id).val()+","
		selectedSearchMembersID +=id;
	}
	else {
		var id = ","+$("#member_" + id).val()+","
		selectedSearchMembersID=selectedSearchMembersID.replace(id, ",");
	}	
}

function selectedGroupMember(id) {
	//單選 groupMember
	if ($("#groupMember_" + id).is(":checked")) {
		var id = $("#groupMember_" + id).val()+","
		selectedGroupMembersID +=id;
	}
	else {
		var id = ","+$("#groupMember_" + id).val()+","
		selectedGroupMembersID=selectedGroupMembersID.replace(id, ",");
	}		
}

function invite_search() {
	var data = {
		 group_id:group_ID,
		 schoolName:$("#newinvite input.p1").val(),
		 grade:$("#newinvite input.p21").val(),
		 className:$("#newinvite input.p22").val(),
		 email:$("#newinvite input.p3").val()
	};
	$.post(url2,data,function(json){ search_list(json); },"json");
	/*search_list({
"response":{
"result":"true",
"members":[
{
"id":5,
"schoolName":"民富國小",
"status":"尚未加入",
"email":"pasta0806@hotmail.com",
"name":"pasta0806",
"className":"二 年 八 班 "
}
]
}
});*/
}

function search_list(json_data) {
	var data = json_data.response;
	if (/true/i.test(data.result)) {
		var trs = "";
		for (var i=0;i<data.members.length;i++) {
			trs += '<tr><td class="is_td0"><input id=member_'+data.members[i].id+' type="checkbox" onclick="selectedSearchMember('+data.members[i].id+')" value='+data.members[i].id+'></td>';			
			trs += '<td class="is_td1">'+data.members[i].name+'</td>';
			trs += '<td class="is_td2">'+data.members[i].email+'</td>';
			trs += '<td class="is_td4">'+data.members[i].schoolName+'</td>';
			trs += '<td class="is_td5">'+data.members[i].className+'</td>';
			trs += '<td class="is_td6">'+data.members[i].status+'</td></tr>';
		}
		$("#searchList").html(trs);
	}
	else { $("#searchList").html("<tr><td colspan='"+$("#searchList").parent().find("thead td").length+"'>"+data.reason+"</td></tr>"); }
}

function reloadPage(){
	 location.replace("/member/configure");
}


function RejectQueue(queue_id){
	if(queue_id>0){
		jQuery.ajax({
			  url: '/member/rejectQueue',
			  type: 'POST',
			  async: false,
			  data: {
				  queue_id:queue_id
			  },
			  success: function(data) {
					  	alert(data);
					  	reloadPage();
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});	
	}
	else{
		alert("發生錯誤");
		return false;
	}	
}

function AddRelation(){
	var email = $("#relationEmail").val();
	if(email!=""){
		if(confirm("  確定新增   "+email+" ？")) {
			jQuery.ajax({
				  url: '/member/addRelation',
				  type: 'POST',
				  async: false,
				  data: {
					email:email
				  },
				  success: function(data) {
						  	alert(data);
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});
		}
	}
	else
		alert("請輸入email!");
}


function DeleteRelation(){
	if(selectedParentsID!=","){
		if(confirm("  確定刪除   所勾選的關係對象(僅會刪除已確認過之對象) ？")) {
			jQuery.ajax({
				  url: '/member/deleteRelation',
				  type: 'POST',
				  async: false,
				  data: {
					parentsId:selectedParentsID
				  },
				  success: function(data) {
						  	alert(data);
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});	
		}
	}
	else{
		alert("未勾選任何帳號。");
		return false;
	}
		
}

function AcceptRelation(queue_id){
	if(queue_id>0){
		jQuery.ajax({
			  url: '/member/acceptRelation',
			  type: 'POST',
			  async: false,
			  data: {
				  queue_id:queue_id
			  },
			  success: function(data) {
					  	alert(data);
					  	reloadPage();
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});	
	}
	else{
		alert("發生錯誤");
		return false;
	}
	
}

function AcceptGroupByGuest(queue_id){
	if(queue_id>0){
		jQuery.ajax({
			  url: '/member/acceptGroupByGuest',
			  type: 'POST',
			  async: false,
			  data: {
				  queue_id:queue_id
			  },
			  success: function(data) {
					  	alert(data);
					  	reloadPage();
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});			
	}
	else{
		alert("發生錯誤");
		return false;
	}
}

function deleteGroupByMember(group_id){
	if (confirm("  確定退出  此班群 ？")) {
		if(group_id>0){
			jQuery.ajax({
				  url: '/member/deleteGroupByMember',
				  type: 'POST',
				  async: false,
				  data: {
					  group_id:group_id
				  },
				  success: function(data) {
						  	alert(data);	
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});	
		}
		else{
			alert("發生錯誤");
			return false;
		}
	}
}

function AddGroup(){
	var GroupName = $("#addGroupName").val();
	if(GroupName!=""){
		if(confirm("  確定新增   "+GroupName+" ？")) {
			jQuery.ajax({
				  url: '/memberGroup/addGroup',
				  type: 'POST',
				  async: false,
				  data: {
					 GroupName:GroupName
				  },
				  success: function(data) {
						  	alert(data);
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});
		}
		
	}
	else
		alert("請輸入班群名稱!");
}

function editGroupName(){
	var GroupName = $("#groupName").val();
	if(GroupName!=""){
		if(confirm("  確定將名稱變更為   "+GroupName+" ？")) {
			jQuery.ajax({
				  url: '/memberGroup/editGroupName',
				  type: 'POST',
				  async: false,
				  data: {
					  group_id:group_ID,
					  GroupName:GroupName
				  },
				  success: function(data) {
						  	alert(data);
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
				});
		}
	}
	else
		alert("請輸入班群名稱!");	
}

function InviteMemberJoinGroup() {
	if(selectedSearchMembersID!=","){
		if(confirm("確定邀請 所勾選的學生加入班群 ？")) {
			jQuery.ajax({
				  url: '/memberGroup/InviteMemberJoinGroup',
				  type: 'POST',
				  async: false,
				  data: {
					  group_id:group_ID,
					  selectedId:selectedSearchMembersID
				  },
				  success: function(data) {
						  	alert(data);
						  	reloadPage();
				  },
				  error: function(xhr, textStatus, errorThrown) { 
					  alert(errorThrown);
				  }
				});	
		}
	}
	else{
		alert("未勾選任何帳號。");
		return false;
	}
}

function AcceptGroupInvite(memberId,groupName){
	if(memberId>0 && (groupName!=null||groupName.equals(""))){
		jQuery.ajax({
			  url: '/memberGroup/AcceptGroupInvite',
			  type: 'POST',
			  async: false,
			  data: {
				  memberId:memberId,
				  groupName:groupName
			  },
			  success: function(data) {
					  	alert(data);
					  	reloadPage();
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});
	}
}

function DeleteMemberFromGroup() {
	if (confirm("  確定將勾選的成員退出  此班群 ？")) {
		jQuery.ajax({
			url : '/memberGroup/deleteMemberFromGroup',
			type : 'POST',
			async : false,
			data : {
				groupId : group_ID,
				selectedGroupMembersID:selectedGroupMembersID
			},
			success : function(data) {
				alert(data);
				reloadPage();
			},
			error : function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});
	}
}

function DeleteGroupByCreator(){
	if (confirm("  確定將刪除 此班群(包含所有成員) ？")) {
		jQuery.ajax({
			url : '/memberGroup/deleteGroupByCreator',
			type : 'POST',
			async : false,
			data : {
				groupId : group_ID	
			},
			success : function(data) {
				alert(data);
				reloadPage();
			},
			error : function(xhr, textStatus, errorThrown) {
				alert(errorThrown);
			}
		});
	}
}



function school_dialog() {
	$.get("/public/javascripts/data/city.txt",function(data){
		eval(data);
		$("div.city ul").html("");
		for (var i=0;i<city.length;i++) { $("div.city ul").append("<li cid='"+city[i].id+"'>"+city[i].name+"</li>"); }
		$("div.city li[cid]").off("click").on("click",function(event){
			var page = $("div.lv2");
			$("div.city li").removeClass("actived");
			$(this).addClass("actived");
			$("div.page").hide();
			$.get("/public/javascripts/data/area_"+$(this).attr("cid")+".txt",function(data){
				eval(data);
				var page1 = $("div.lv3");
				$("div.zone ul").html("");
				$("div.page").hide();
				for (var i=0;i<area.length;i++) { $("div.zone ul").append("<li aid='"+area[i].id+"'>"+area[i].name+"</li>"); }
				$("div.zone li[aid]").off("click").on("click",function(event){
					$("div.zone li").removeClass("actived");
					$(this).addClass("actived");
					$("div.page").hide();
					$.get("/public/javascripts/data/school_"+$("div.city li.actived").attr("cid")+".txt",function(data){
						eval(data);
						$("div.school ul").html("");
						for (var i=0;i<school.ele.length;i++) { if (school.ele[i].area_id==$("div.zone li.actived").attr("aid")) { $("div.school ul.ele").append("<li>"+school.ele[i].name+"</li>"); } }
						for (var i=0;i<school.jun.length;i++) { if (school.jun[i].area_id==$("div.zone li.actived").attr("aid")) { $("div.school ul.jun").append("<li>"+school.jun[i].name+"</li>"); } }
						$("div.school li").off("click").on("click",function(event){
							$("div.school li").removeClass("actived");$(this).addClass("actived");
							$("#school_full_name").val($("div.city li.actived").html()+$("div.zone li.actived").html()+"-"+$(this).html());
							$("#school").modal("toggle");
						});
						$(page1).show();
					});
				});
				$(page).show();
			});
		});
	});
}