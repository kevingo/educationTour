var url1 = "/ajax/getExamineInfo";//讀取題目
var url2 = "/examine/fillExamine";//提交問卷
var exam_list;
var seq = 0;
var myans = [];
var exam_ID = 0;
$(function(){
	$.post(url1,{examId: $("#examID").val()},function(json){
		var data = json.response;
		$("span.ed_exam_title").html(data.name);
		exam_list = data.questions;
		start_to_exam();
		$("a.next").on("click",function(event){
			var pass1 = ($(".exam_detail_content input:radio").length>0)?($(".exam_detail_content input:radio:checked").length>0):false;
			var pass2 = ($(".exam_detail_content textarea").length>0)?($(".exam_detail_content textarea").val()!=""):false;
			if (pass1||pass2) {
				save_ans();
				seq++;
				start_to_exam();
			}
			else {
				$("#tips").modal("toggle");
			}
		});
		$("a.back").on("click",function(event){
			save_ans();
			seq--;
			start_to_exam();
		});
		$("a.finish").on("click",function(event){
			var pass1 = ($(".exam_detail_content input:radio").length>0)?($(".exam_detail_content input:radio:checked").length>0):false;
			var pass2 = ($(".exam_detail_content textarea").length>0)?($(".exam_detail_content textarea").val()!=""):false;
			if (pass1||pass2) {
				save_ans();
				$("body").append('<div id="status">提交問卷中....</div><div class="modal-backdrop fade in" id="processing"></div>');
				$.post(url2,{oaID: $("#oaID").val() ,examID: $("#examID").val(), ans:myans},function(data){ 
					setTimeout(function(){ $("#status,#processing").remove(); 
					window.location.href='/examine/showResult/'+data.response.id; 
					},2000); });
			}
			else {
				$("#tips").modal("toggle");
			}
		});
	});
});
function start_to_exam() {
	if (seq==0) {$("a.back").hide();}
	else {$("a.back").show();}
	if (seq<exam_list.length-1) {$("a.next").show();}
	else {$("a.next").hide();}
	if (seq==exam_list.length-1) {$("a.finish").show();}
	else {$("a.finish").hide();}
	$("div.exam_detail_content").html("Loading...Please wait...");
	var cname = new Array("e02", "e01", "e03");
	$.get("/public/javascripts/data/qtype_"+cname[exam_list[seq].style]+".txt",function(data){
		$("div.exam_detail_content").html(data);
		$("span.ere_number").html(seq+1);
		$("span.ere_points").html(((exam_list[seq].score!=null)?exam_list[seq].score:"0")+"分");
		$("span.status").html("第"+(seq+1)+"/"+exam_list.length+"題");
		$("div.ere_question").html(exam_list[seq].question);
		if (exam_list[seq].fileName!=null && exam_list[seq].fileName!="") { $("div.ere_question").append('<a class="btn btn-mini" href='+exam_list[seq].fileName+'>題目附件檔下載</a>'); }
		if (cname[exam_list[seq].style]=="e01") {
			$("div.ere_answers span.ans_option:first").html(exam_list[seq].optionA);
			$("div.ere_answers span.ans_option:eq(1)").html(exam_list[seq].optionB);
			$("div.ere_answers span.ans_option:eq(2)").html(exam_list[seq].optionC);
			$("div.ere_answers span.ans_option:last").html(exam_list[seq].optionD);
		}
		get_ans();
	});
}
function save_ans() {
	var ans = ($("textarea").length>0)?$("textarea").val():$("input:radio:checked").val();
	if (seq>(myans.length-1)) {
		myans.push(ans);
	}
	else { myans[seq] = ans; }
}
function get_ans() {
	if (seq<=(myans.length-1)) {
		if ($(".ere_answers textarea").length>0) { $(".ere_answers textarea").val(myans[seq]); }
		else { $(".ere_answers input[value='"+myans[seq]+"']").prop("checked",true); }
	}
}