var url1 = "/ajax/getExamineAnswerInfo";
var cname = new Array("e02","e01","e03");
$(function(){
	$.post(url1,{ansId: $("#ansId").val()},function(json){
		var data = json.response;
		if(data.result=="true"){
			var qq = data.questions;
			var isTeacher = data.isTeacher;
			$(".er_score_result").html(parseInt(data.bonusScore,10)+parseInt(data.score,10));
			//$(".illustration").html(data.illustration);
			load_view_question(cname[qq[0].style],qq,0,isTeacher);
		}
		
	});
});
function load_view_question(class_name, json, index,isTeacher) {
	//alert(isTeacher);
	$.get("/public/javascripts/data/qview_"+class_name+".txt",function(data){
		$(".er_exams").append(data);
		var div = $(".er_exam_item:last");
		$(div).find(".qid").val(json[index].id);
		$(div).find(".ere_number").html($(".er_exam_item").length);
		$(div).find(".ere_points").html(json[index].score+"分");
		$(div).find(".ere_question").append(json[index].question);
		if (json[index].filePath!=null) { $(div).find(".ere_question").append("<p><a class='btn btn-mini' href='"+json[index].filePath+"'>參考附件檔下載："+json[index].fileName+"</a></p>"); }
		$(div).find(".ere_about_answer").append(json[index].illustration);
		if (json[index].teacherComment) {
			$(div).find(".ere_teacher_words").append('<ul><li class="ere_teacher_word_old"><span class="date">'+json[index].teacherCommentTime+'</span><span class="reply">'+json[index].teacherComment+'</span><span class="point"></span></li></ul>');
			$(div).find(".teacher_comment").remove();
		}
		else if(isTeacher){	
			//alert(isTeacher);
			$(div).find(".btn").off("click").on("click",function(event){
				var pa = $(this).parents(".er_exam_item");
				var questionId = $(pa).find(".qid").val();//question id
				var score = ($(pa).find(".extra").length>0)?$(pa).find(".extra").val():0;//score
				var comment_words = $(pa).find(".comment").val();
				$(pa).find(".comment_error").remove();
				if (/^$/.test(comment_words)) {
					$(pa).find(".comment").addClass("error");
					$(pa).find(".btn").after("<label class='comment_error'>回覆或評語不能空白</label>");
				}
				else { commentExamine(questionId,score,comment_words); }
				event.stopPropagation();
			});
		}
		else{
			//alert(isTeacher);
			$(div).find(".ere_teacher_words").append('<ul><li class="ere_teacher_word_old"><span class="date">無</span><span class="reply"></span><span class="point"></span></li></ul>');
			$(div).find(".teacher_comment").remove();
		}
		if (class_name=="e01") {
			$(div).find(".ans_option:first").html(json[index].optionA);
			$(div).find(".ans_option:eq(1)").html(json[index].optionB);
			$(div).find(".ans_option:eq(2)").html(json[index].optionC);
			$(div).find(".ans_option:last").html(json[index].optionD);
			$(div).find("input:radio[value='"+json[index].answer+"']").parent("p").addClass("ere_answers_correct");
			
			$(div).find("input:radio[value='"+json[index].studentAnswer+"']").prop("checked",true);
			if (parseInt(json[index].answer,10)!=parseInt(json[index].studentAnswer,10)) { $(div).find(".ere_points").after("<span class='ere_check check_no'></span>"); }
			else { $(div).find(".ere_points").after("<span class='ere_check check_yes'></span>"); }
		}
		else if(class_name=="e02") {
			$(div).find("input:radio[value='"+json[index].answer+"']").parent("p").addClass("ere_answers_correct");
			$(div).find("input:radio[value='"+json[index].studentAnswer+"']").prop("checked",true);
			if (parseInt(json[index].answer,10)!=parseInt(json[index].studentAnswer,10)) { $(div).find(".ere_points").after("<span class='ere_check check_no'></span>"); }
			else { $(div).find(".ere_points").after("<span class='ere_check check_yes'></span>"); }
		}
		else { $(div).find(".ere_answers").append(json[index].studentAnswer); }
		if (json) { if (index<(json.length-1)) { load_view_question(cname[json[index+1].style],json,index+1,isTeacher); } }
	});
}


function commentExamine(questionId,score,comment_words){
	var ansid = $("#ansId").val()
	if(ansid!=""){
		jQuery.ajax({
			  url: '/examine/comment',
			  type: 'POST',
			  async: false,
			  data: {
				  ansId:ansid,
				  questionId:questionId,
				  comment:comment_words,
				  bonusScore:score
			  },
			  success: function(data) {
					  	alert(data);
					  	reloadPage(ansid);
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});	
	}
}

function reloadPage(ansid){
	location.replace("/examine/showResult/"+ansid);
}

function gotoMyHomePage(){
	location.replace("/member/myHomePage");
}

function finishExamine(){
	var ansid = $("#ansId").val()
	if(confirm("  確定已完成閱卷   ？")) {
		jQuery.ajax({
			  url: '/examine/finish',
			  type: 'POST',
			  async: false,
			  data: {
				  ansId:ansid,
			  },
			  success: function(data) {
					  	alert(data);
					  	gotoMyHomePage();
			  },
			  error: function(xhr, textStatus, errorThrown) { alert(errorThrown); }
			});
	}
}