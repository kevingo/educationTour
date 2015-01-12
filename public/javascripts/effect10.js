var url = "/ajax/getContactInfo";//ajax post url
var url2 ="/ajax/getTagInfo"
var TagId=0;
var ContactId=0;
$(function(){
	
	//change tab
	var tab_name = window.location.href.split("#");
	if (tab_name.length>1) { 
		$("#tagmanage").removeClass("active");
		$("#"+tab_name[1]).addClass("active");
		$("a[href='#tagmanage']").parents("li").removeClass("active");
		$("a[href='#"+tab_name[1]+"']").parents("li").addClass("active");
		
		$("#gotop").trigger("click");
		//$("a[href='#"+tab_name[1]+"']").click();
	}
	
	
	$("#reply_list").find("button.btn-info").on("click",function(event){
		$("#reply_list tr.replydetail").remove();
		var tr = $(this).parents("tr");
		$.get("/public/javascripts/data/reply_content.txt",function(data){
			$(tr).after(data);
			$.post(url, {cid:ContactId}, function(json){tr_assign_data(json);},"json");
			//tr_assign_data(null);
		});
		event.stopPropagation();
	});
	getTagCategoryOnEdit();
	getTagCategoryOnDelete();
	getTagCategoryOnEditTag();
});
function tr_assign_data(json) {
	if (json && json.response.result) {
		$("tr.replydetail select").val(json.response.contact.status);
		$("tr.replydetail div.user_reply div").html(json.response.contact.content);
		$("tr.replydetail textarea.editor").val(json.response.contact.re_content);
	}
	tinymce.init({
	    selector:'textarea.editor',
	    plugins: [
	        "advlist autolink lists link image charmap print preview hr anchor pagebreak",
	        "searchreplace wordcount visualblocks visualchars code fullscreen",
	        "insertdatetime media nonbreaking save table contextmenu directionality",
	        "emoticons template paste textcolor"
	    ],
	    toolbar1: "insertfile undo redo | styleselect | forecolor backcolor bold italic | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image"
	});
}

function cancelReply(){
	$("#reply_list tr.replydetail").remove();
	var tr = $(this).parents("tr");
	setContactId(0);
}

function replyContact(){
	
	var content = tinymce.activeEditor.getContent({format : 'raw'});
	var status =$("#replyContactStatus :selected").text();
	if (content == null || content == "") {
		alert("請輸入回覆內容");
	} else {
		jQuery.ajax({
			url : '/admin/replyContact',
			type : 'POST',
			async : false,
			data : {
				contactId : ContactId,
				replyContent : content,
				status:status			
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

function reloadPage(){
	 location.replace("/admin/index");
}

function newActivity(){
	var activityName= $("#newActivityName").val();
	if(activityName==null || activityName=="")
		alert("請輸入標籤大類別名稱");
	else{
		jQuery.ajax({
			url : '/tag/createTagActivity',
			type : 'POST',
			async : false,
			data : {
				activityId:'0',
				activityName : activityName	
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

function editActivity(){
	var a_id = $('#editSelectTagActivity :selected').val();
	var activityName= $("#editTagActivityName").val();
	
	if(activityName==null || activityName=="")
		alert("請輸入標籤大類別名稱");
	else{
		jQuery.ajax({
			url : '/tag/createTagActivity',
			type : 'POST',
			async : false,
			data : {
				activityId:a_id,
				activityName : activityName	
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

function deleteActivity(){
	var a_id = $('#deleteSelectTagActivity :selected').val();
	var a_name = $('#deleteSelectTagActivity :selected').text();
	if(a_id==null || a_id=="")
		alert("請選擇標籤大類別");
	else{
		if(confirm("  確定刪除大類別   "+a_name+" ？")) {
			jQuery.ajax({
				url : '/tag/deleteTagActivity',
				type : 'POST',
				async : false,
				data : {
					activityId:a_id
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
}

function newTagCategory(){
	var a_id = $('#newCategorySelectTagActivity :selected').val();
	var categoryName= $("#newTagCategoryName").val();
	if(a_id==null || a_id=="" ||categoryName==null ||categoryName=="")
		alert("請選擇標籤大類別與填入標籤類別內容");
	else{
			jQuery.ajax({
				url : '/tag/createTagCategory',
				type : 'POST',
				async : false,
				data : {
					activityId:a_id,
					categoryId:'0',
					categoryName:categoryName
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


function editTagCategory(){
	var a_id = $('#editCategorySelectTagActivity :selected').val();
	var c_id = $('#editCategorySelectTagCategory :selected').val();
	var categoryName= $("#editTagCategoryName").val();
	if(a_id==null || a_id=="" ||categoryName==null ||categoryName=="")
		alert("請選擇標籤大類別與填入標籤類別內容");
	else{
			jQuery.ajax({
				url : '/tag/createTagCategory',
				type : 'POST',
				async : false,
				data : {
					activityId:a_id,
					categoryId:c_id,
					categoryName:categoryName
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

function deleteTagCategory(){
	var c_id = $('#deleteCategorySelectTagCategory :selected').val();
	var categoryName= $("#deleteCategorySelectTagCategory").text();
	if(c_id==null || c_id=="")
		alert("請選擇標籤類別");
	else{
		if(confirm("  確定刪除此分類   "+categoryName+" ？")) {
			jQuery.ajax({
				url : '/tag/deleteTagCategory',
				type : 'POST',
				async : false,
				data : {
					categoryId:c_id
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
}

function getTagCategoryOnEdit(){
	var activity = $('#editCategorySelectTagActivity :selected').val();
	
	if(activity>0){
		jQuery.ajax({
			  url: '/ajax/getTagCategory',
			  type: 'POST',
			  async: false,
			  data: {
			  	activity:activity
			  },
			  success: function(response) {
					  $("#editCategorySelectTagCategory").html(response);				  		  
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});
	}
	else{
		alert("請先選擇大類別");
	}	
}

function getTagCategoryOnDelete(){
	var activity = $('#deleteCategorySelectTagActivity :selected').val();
	
	if(activity>0){
		jQuery.ajax({
			  url: '/ajax/getTagCategory',
			  type: 'POST',
			  async: false,
			  data: {
			  	activity:activity
			  },
			  success: function(response) {
					  $("#deleteCategorySelectTagCategory").html(response);				  		  
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});
	}
	else{
		alert("請先選擇大類別");
	}
		
}

function getTagCategoryOnEditTag(){
	var activity = $('#editTagSelectActivity :selected').val();
	
	if(activity>0){
		jQuery.ajax({
			  url: '/ajax/getTagCategory',
			  type: 'POST',
			  async: false,
			  data: {
			  	activity:activity
			  },
			  success: function(response) {
					  $("#editTagSelectCategory").html(response);				  		  
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});
	}
	else{
		alert("請先選擇大類別");
	}
		
}

function  setTagId(tagId){
	TagId=tagId;
	//帶入資料
	if(TagId>0){
		$.post(url2,{tagId:TagId},function(json){ setTagInfo(json); },"json");
	}
	else{
		$("#editTagName").val("");
		$("#editTagColor").val("");
	}
		
}

function setTagInfo(json){
	if (json) {
		if (json.response.result) {
			$("#editTagName").val(json.response.tag.name);
			$("#editTagColor").val(json.response.tag.color);
			//set選取Activity
			var opt = $("#editTagSelectActivity option[value="+json.response.tagActivity+"]"),
		    html = $("<div>").append(opt.clone()).html();
			html = html.replace(/\>/, ' selected="selected">');
			opt.replaceWith(html);
			//撈出category
			getTagCategoryOnEditTag();
			//set選取category
			opt = $("#editTagSelectCategory option[value="+json.response.tagCategory+"]"),
		    html = $("<div>").append(opt.clone()).html();
			html = html.replace(/\>/, ' selected="selected">');
			opt.replaceWith(html);
			
			//set選取 enable
			var enable= (json.response.tag.enable)?1:0;
			opt = $("#editTagEnable option[value="+enable+"]"),
		    html = $("<div>").append(opt.clone()).html();
			html = html.replace(/\>/, ' selected="selected">');
			opt.replaceWith(html);
		}
	}
}

function createTag(){
	var name = $("#editTagName").val();
	var color = $("#editTagColor").val();
	var activityId = $('#editTagSelectActivity :selected').val();
	var categoryId = $('#editTagSelectCategory :selected').val();
	var enable = $('#editTagEnable :selected').val();
	
	if(name==null || name=="" || color==null ||color=="")
		alert("請輸入全部欄位。");
	else{
		if(confirm("  確定新增(修改)標籤   "+name+" ？")) {
			jQuery.ajax({
				url : '/tag/createTag',
				type : 'POST',
				async : false,
				data : {
					t_id:TagId,
					name:name,
					color:color,
					categoryId:categoryId,
					enable:enable				
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
	
}

function deleteTag(){
	if(TagId>0){
		jQuery.ajax({
			url : '/tag/deleteTag',
			type : 'POST',
			async : false,
			data : {
				t_id:TagId				
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

function setContactId(contactId){
	ContactId=contactId;
}


