var url1="";
var url2 = "";
/*
$(function(){
	
	//teacher change
	$("#teachers").on("change",function(event){
		alert("do something..");
		event.stopPropagation();
	})
	//active chage
	$("#actives").on("change",function(event){
		alert("do something..");
		event.stopPropagation();
	});
	//search change
	$("#search_keyword").on("keyup",function(event){
		if(event.keyCode==13) {//press enter
			alert("do something...");
		}
		event.stopPropagation();
	});
	events();
});
*/
function events() {
	//exam list
	$("#exam_list tr").find("td:not(td.mse_td8)").on("click",function(event){
		if ($(this).parent().next("tr.exam_detail").length>0) {
			$(this).parent().next("tr.exam_detail").remove();
		}
		else {
			var tr = $(this).parent();
			$.get("/public/javascripts/data/exam_table.txt",function(data){
				$(tr).after(data);
				//$.post(url2,{id:$(tr).find("td.mse_td1").html()},function(json)}{ get_data_list(tr, json); },"json");
				get_data_list(tr,[{
					id:"1",
					name:"王大同",
					begin:"2013/10/08 13:15:11",
					finish:"2013/10/09 13:15:11",
					result:90
				},{
					id:"2",
					name:"陳小明",
					begin:"2013/10/08 13:15:11",
					finish:"2013/10/09 13:15:11",
					result:100
				}]);
			});
		}
		event.stopPropagation();
	});
}
function get_data_list(obj, json_data) {
	var tr = $(obj).next("tr.exam_detail");
	var tbody = $(tr).find(".std_list");
	if (json_data) {
		var html = "";
		for (var i=0;i<json_data.length;i++) {
			html += "<tr><td class='es1_td1'>"+json_data[i].name+"</td>";
			html += "<td class='es1_td1'>"+json_data[i].begin+"</td>";
			html += "<td class='es1_td1'>"+json_data[i].finish+"</td>";
			html += "<td class='es1_td1'>"+json_data[i].result+"</td>";
			html += "<td class='es1_td5'><button class='btn btn-mini btn-success' std_id='"+json_data[i].id+"'>閱卷</button></td></tr>";
		}
		$(tbody).html(html);
		$(tbody).find("button.btn").off("click").on("click",function(event){
			alert("forward???!!!");
			event.stopPropagation();
		});
	}
	else {
		$(tbody).html("<tr><td colspan='5'>No Data.</td></tr>");
	}
}