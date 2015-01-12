$(function(){
	$("input.title").off("click").on("click",function(event){
		$("div.title").hide();
		$("div."+$(this).attr("class").replace(/\stitle/i,"")).find("input").val("");
		$("div."+$(this).attr("class").replace(/\stitle/i,"")).find("span.check").remove();
		$("div."+$(this).attr("class").replace(/\stitle/i,"")).fadeIn("fast");
	});
	$("button.chose_school").off("click").on("click",function(event){
		$("div.page").hide();
		school_dialog();
		$("div.city").show();
		$('#school').modal("toggle");
	});
	//birthday
	$('#dp3').datepicker();
	//previous page
	$("button.prev").off("click").on("click",function(event){
		var obj = $(this).parent("div.page");
		$("div.page").hide();
		$(obj).prev("div.page").fadeIn("fast");
	});
	//reg
	$("button.regInfo").off("click").on("click",function(event){
		$("span.check").remove();
		check_format($("#reg_mail"));
		check_format($("input.username"));
		check_format($("input.pwd1"));
		check_format($("input.gender"));
		check_format($("input.title"));
		check_format($("#birthday"));
		$("div.title:not(:hidden)").find("input").each(function(i){
			if ($(this).hasClass("email")) {
				if (!/^\s*$/i.test($(this).val())) { check_format($(this)); }
			}
			else {check_format($(this));}
		});
		check_format($("#agree"));
		if ($("span.checkno").length<1) {
			$("#regFM").submit();
		}
	});
	
	//cancel
	$("button.regCancel").off("click").on("click",function(event){
		 location.replace("/member/register");
	});
});
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