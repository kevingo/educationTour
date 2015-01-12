var url1 = "/ajax/getMemberGroupByTeacher";
var url2 = "/ajax/getMemberListByMemberGroup";
$(function(){
	//tinyeditor
	tinymce.init({
        selector:'#mail_content',
        plugins: [
            "advlist autolink lists link image charmap print preview hr anchor pagebreak",
            "searchreplace wordcount visualblocks visualchars code fullscreen",
            "insertdatetime media nonbreaking save table contextmenu directionality",
            "emoticons template paste textcolor"
        ],
        toolbar1: "insertfile undo redo | styleselect | forecolor backcolor bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image",
        setup: function(editor) { editor.on('keyup', function(e) { need_to_save(); }); }
    });
	//class list
	$.post(url1,null,function(json){ class_list(json); },"json");
	//select all
	$("thead :checkbox").on("click",function(event){
		if ($(this).is(":checked")) {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", true);
		}
		else {
			$(this).parents("table.table").find("tbody :checkbox").prop("checked", false);
		}
	});
	//select ok
	$("a.ok").on("click",function(event){
		$("#member_list :checkbox:checked").each(function(i){
			var mail_addr = $(this).parents("tr").find("td:eq(2)").html();
			var name = $(this).parents("tr").find("td:eq(1)").html();
			add_mail_list(name,mail_addr);	
			//取消選取名單的對話框
			$("#btnCancelList").click();
		});
	});
	//keyin mail address
	$("#manual_mail_addr").on("keyup",function(event){
		var sep = [32,186,188];
		if (jQuery.inArray(event.keyCode,sep)>=0) {
			var string = $(this).val().substring(0,$(this).val().length-1);
			if (/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/i.test(string)) {
				add_mail_list(string,string);
			}
			$(this).val(null);
		}
		event.stopPropagation();
	});
	//delete list
	assign_del_mail_list();
	//double check
	$("#double_check").on("click",function(event){
		$("#sendconfirm span.mail_counts").html($(".send_mail_list li").length);
		$("#sendconfirm").modal("toggle");
	});
	//submit
	$("a.submit").on("click",function(event){
		
		$(window).off("beforeunload");
		if($(".send_mail_list li").length>0)
			$("#myForm").submit();
		else
			alert("至少須有一位收件人");
	});
	//on modify
	$("input.mail_subject").one("keyup",function(event){ need_to_save(); });
	//give up content
	$("a.without_saving").on("click",function(event){
		$(window).off("beforeunload");
		window.location.href='/';
	});
});
function class_list(json_data) {
	var data = json_data.response;
	if (/true/i.test(data.result)) {
		$("div.class_list ul").html("");
		var lis = "";
		for (var i=0;i<data.groups.length;i++) {
			lis += "<li><a group_id='"+data.groups[i].id+"'>"+data.groups[i].name+"<span class='stu_number'>("+data.groups[i].memberSize+")</span></a></li>";
		}
		$("div.class_list ul").html(lis);
		$("div.class_list li a").off("click").on("click",function(event){
			group_ID = $(this).attr("group_id");
			$.post(url2,{group_id:group_ID},function(json){ Member_list(json); },"json");
		});
	}
}
function Member_list(json_data) {
	var data = json_data.response;
	if (/true/i.test(data.result)) {
		var trs = "";
		if(data.members.length==0){
			$("#member_list").html("<tr><td colspan='"+$("#member_list").parent().find("thead td").length+"'>No Data.</td></tr>");
		}
		else{
			for (var i=0;i<data.members.length;i++) {
				trs += '<tr><td class="mc_td1"><input id=groupMember_'+data.members[i].id+' type="checkbox" onclick=selectedGroupMember('+ data.members[i].id +') value='+data.members[i].id+'></td>';
				trs += '<td class="mc_td2">'+data.members[i].name+'</td>';
				trs += '<td class="mc_td3">'+data.members[i].email+'</td></tr>';
			}
			$("#member_list").html(trs);
		}
	}
	else { $("#member_list").html("<tr><td colspan='"+$("#member_list").parent().find("thead td").length+"'>"+data.reason+"</td></tr>"); }
}
function assign_del_mail_list() { $(".send_mail_list li span.del").off("click").on("click",function(event){ $(this).parent("li").remove(); }); }
function add_mail_list(name,mail_addr) {
	$(".send_mail_list").append("<li>"+name+"<span class='del btn btn-mini'>X</span><input type='hidden' name='rec_name[]' value='"+name+"'><input type='hidden' name='rec_mail[]' value='"+mail_addr+"'></li>");
	need_to_save();
	assign_del_mail_list();
}
function need_to_save() {
	$(window).off("beforeunload").on("beforeunload",function(event){ return "你確定要離開此頁面嗎？(不儲存離開)"; });
}