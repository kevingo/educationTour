$(function(){
	$("#action_filter a").on("click",function(event){
		event.preventDefault();
		var div_id = $(this).attr("href").replace("#","");
		$("#action_filter").find("li").removeClass("active");
		$(this).parent().addClass("active");
		$("#action_div").children("div").hide();
		$("#"+div_id).show();
	});
	
	$('.nc210x155').nailthumb({width:210,height:155});

});






