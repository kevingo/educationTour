var cname = new Array("e02","e01","e03");
var url1 = "/ajax/getExamineInfo";
$(function(){
	$("#timepicker1, #timepicker2").timepicker({ 'timeFormat': 'H:i','step': 10,'scrollDefaultNow': true });
	//bootstrap datepicker
	var nowTemp = new Date();
	var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
	var checkin = $('#dpd1').datepicker({
	  onRender: function(date) {
	    return date.valueOf() < now.valueOf() ? 'disabled' : '';
	  }
	}).on('changeDate', function(ev) {
	  if (ev.date.valueOf() > checkout.date.valueOf()) {
	    var newDate = new Date(ev.date)
	    newDate.setDate(newDate.getDate() + 1);
	    checkout.setValue(newDate);
	  }
	  checkin.hide();
	  $('#dpd2')[0].focus();
	}).data('datepicker');
	var checkout = $('#dpd2').datepicker({
		onRender: function(date) { return date.valueOf() <= checkin.date.valueOf() ? 'disabled' : ''; }
	}).on('changeDate', function(ev) { checkout.hide(); }).data('datepicker');
	//add exam type
	$("#add_exam li").on("click",function(event){ $("#add_exam li").removeClass("active"); $(this).addClass("active"); $("#append_exam").show(); });
	$("#append_exam").on("click",function(event){
		if ($("#add_exam li.active").length>0) {
			load_exam_content($("#add_exam li.active").attr("class").match(/e\d\d/),null);
			need_to_save();
			$("#add_exam").modal("toggle");
			$("#add_exam li").removeClass("active");
			$(this).hide();
		}
	});
	//tab click
	$('#create_exam_tab a').click(function (e) { e.preventDefault(); $(this).tab('show'); });
	//next tab
	$("a.next").on("click",function(event){ $("#create_exam_tab li:last a").trigger("click"); });
	//prev tab
	$("a.prev").on("click",function(event){ $("#create_exam_tab li:first a").trigger("click"); });
	//save button
	
	$("a.save").on("click",function(event){
	  $("span.check").remove();
	  $(".error").removeClass("error");
	  $("input.require").each(function(i){
		   if (/^$/.test($(this).val())) { $(this).after("<span class='pull-right check checkno'></span>"); $(this).addClass("error"); }
		  });
	  $("textarea.require").each(function(i){
		  if (/^$/.test($(this).val())) { $(this).parents("div.editor").append("<span class='pull-right check checkno'></span>"); $(this).addClass("error"); }
	  });
	  if ($("div.exam").length<1) {
	   alert("至少需1題目才能完成儲存!");
	  }
	  else {
		  if ($("span.checkno").length<1) {
		    listen_modify();
		    $("body").append('<div id="status">Saving....Please Wait</div><div class="modal-backdrop fade in" id="processing"></div>');
		    $("#basic_ifm,#question_ifm").remove();
		    $("body").append("<iframe id='basic_ifm' name='basic_ifm'></iframe><iframe id='question_ifm' name='question_ifm'>");
		    $("#basicFM").submit();
		    $("#basic_ifm").load(function(){
		    	$("#questionFM").submit();
		    	$("#question_ifm").load(function(){
		    		setTimeout(function(){ $("#status,#processing,#basic_ifm,#question_ifm").remove(); },2000);
		    	});
		    });
		  }
		  else
			  alert("請確認所有必填資料均已完成!");
	  }
	});
	//delete question
	$("#deleit").on("click",function(event){
		$("a.sureDelete").parents("div.exam").remove();
		need_to_save();
		get_newSeq();
		get_newScore();
		$("#double").modal("toggle");
		event.stopPropagation();
	});
	//stand by to check modify
	listen_modify();
	//revise data
	if (/\/examine\/create\/\d+$/.test(window.location.href)) {
		var value = window.location.href.split("/");
		$("#examID").val(value[value.length-1]);
		$.post(url1,{examId: $("#examID").val()},function(json){
			$("#exam_title").val(json.response.name);
			$("select[name='oaid'] option[value='"+(json.response.oaid)+"']").prop("selected",true);
			$("select[name='publish'] option[value='"+((json.response.publish)?1:0)+"']").prop("selected",true);
			$("select[name='status'] option[value='"+((json.response.status)?1:0)+"']").prop("selected",true);
			$("#dpd1").val(json.response.startDate);
			$("#timepicker1").val(json.response.startTime);
			$("#dpd2").val(json.response.endDate);
			$("#timepicker2").val(json.response.endTime);
			$("#basic textarea").val(json.response.introduction);
			$("#examcontent .basic_info:first textarea").val(json.response.explain);
			$("#examcontent .basic_info:last textarea").val(json.response.illustration);
			load_exam_content(cname[json.response.questions[0].style], json.response.questions, 0);
		},"json");
	}
});
function load_exam_content(class_name,json,index) {
	//alert(json[index].id);
	$.get("/public/javascripts/data/exam_"+class_name+".txt",function(data){
		$("#exam_items").append(data);
		$("div.exam:last span.exam_number").html($("div.exam").length);
		$("div.exam:last input.exam_number").val($("div.exam").length);
		if (json) {
			if (class_name == "e01") {
				$("div.exam:last div.qas input:text:first").val(json[index].optionA);
				$("div.exam:last div.qas input:text:eq(1)").val(json[index].optionB);
				$("div.exam:last div.qas input:text:eq(2)").val(json[index].optionC);
				$("div.exam:last div.qas input:text:last").val(json[index].optionD);
				$("div.exam:last div.qas input:radio[value='"+json[index].answer+"']").prop("checked",true);
				$("div.exam:last input.answer").val(json[index].answer);
				$("div.exam:last input.score").val(json[index].score);
			}
			else if(class_name == "e02") {
				$("div.exam:last div.qas input:radio[value='"+json[index].answer+"']").prop("checked",true);
				$("div.exam:last input.answer").val(json[index].answer);
				$("div.exam:last input.score").val(json[index].score);
			}
			$("div.exam:last input.exam_id").val(json[index].id);
			$("div.exam:last input.fileName").val(json[index].fileName);
			$("div.exam:last textarea:first").val(json[index].question);
			$("div.exam:last textarea:last").val(json[index].illustration);
			if (index<(json.length-1)) { load_exam_content(cname[json[index+1].style], json, index+1); }
			else { listen_modify(); get_newScore(); }
		}
		common_event($("div.exam:last"));
		if ($("div.exam").length>1) { $("p:has(a.addfiled)").show(); }
		$(".exam_total_count").html($("div.exam").length);
	});
}
function common_event(obj) {
	
	$(obj).find("input:radio").each(function(i){
		var newname = $(this).attr("name")+$(this).parents("div.exam").index();
		$(this).attr("name", newname);
	});
	$(obj).find("input:radio").off("click").on("click",function(event){
		$(this).parents("div.exam").find("input.answer").val($(this).val());
		event.stopPropagation();
	});
	
	//button event
	$(obj).find("a.delete").off("click").on("click",function(event){
		$("a.delete").removeClass("sureDelete");
		$(this).addClass("sureDelete");
		$("#double").modal("toggle");
	});
	$(obj).find("a.upup").off("click").on("click",function(event){
		var self = $(this).parents("div.exam");
		if ($(self).index()>0) {
			var prev = $(self).prev("div.exam");
			$(self).insertBefore($(prev));
			need_to_save();
			get_newSeq();
			reinit_cleditor($(self));
		}
	});
	$(obj).find("a.downdown").off("click").on("click",function(event){
		var self = $(this).parents("div.exam");
		if ($(self).index()<($("div.exam").length-1)) {
			var nt = $(self).next("div.exam");
			$(self).insertAfter($(nt));
			need_to_save();
			get_newSeq();
			reinit_cleditor($(self));
		}
	});
	$(obj).find(".score").off("keyup").on("keyup",function(event){
		if (/^\d+$/.test($(this).val())) {
			get_newScore();
			need_to_save();
		}
		event.stopPropagation();
	});
	$(obj).find("a.fileTrigger").off("click").on("click",function(event){
		var div = $(this).parents(".exam");
		$(div).find("input:file").trigger("click");
	});
	$(obj).find("input:file").off("change").on("change",function(event){
		if (/\.(pdf|doc|docx|jpg|png|gif|bmp)$/i.test($(this).val())) {
			var div = $(this).parents(".exam");
			$(div).find("input.fileName").val($(this).val());
		}
		else { $(this).val(""); }
	});
	//cleditor
	$(obj).find("textarea").cleditor();
	$(obj).find("textarea").each(function(i){
		$(this).prev().css("height","54px");
		$(this).next("iframe").css("width","100%");
		//keyup event
		$(this).next("iframe").contents().find("body").one("keyup",function(){
			need_to_save();
			$("#examcontent iframe").contents().find("body").off("keyup",need_to_save);
		});
	});
}
function get_newSeq() {
	$("div.exam").each(function(i){
		$(this).find("span.exam_number").html(i+1);
		$(this).find("input.exam_number").val(i+1);
	});
	$(".exam_total_count").html($("div.exam").length);
}
function get_newScore() {
	var total = 0;
	$("div.exam .score").each(function(i){ total += parseInt($(this).val(),10); });
	$(".exam_score").html(total);
}
function listen_modify() {
	$(window).off("beforeunload");
	//$("a.save").hide();
	$("input:text,textarea").off("keyup",need_to_save).one("keyup",need_to_save);
	$("select,input:file").off("change",need_to_save).one("change",need_to_save);
}
function exit_without_save() {
	$(window).off("beforeunload").on("beforeunload",function(event){ return "你確定要離開此頁面嗎？(不儲存離開)"; });
	$("a.save").show();
}
function need_to_save() {
	if ($("a.save").is(":hidden")) {
		exit_without_save();
		$("input:text,textarea").off("keyup",need_to_save);
		$("select,input:file").off("change",need_to_save);
	}
}
function reinit_cleditor(div) {
	//remove cleditor
	$(div).find("textarea").each(function(i){
		var editor = $(this).cleditor()[0];
		editor.$area.insertBefore(editor.$main); // Move the textarea out of the
		editor.$area.removeData("cleditor"); // Remove the cleditor pointer from the
		editor.$main.remove(); // Remove the main div and all children from the DOM
	});
	$(div).find("textarea").cleditor();
	$("textarea.tinyeditor").each(function(i){ $(this).next("iframe").css("width","100%"); });
}