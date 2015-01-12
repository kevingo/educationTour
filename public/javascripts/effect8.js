$(function(){
	//personal profile save
	$("#picupload .btn-primary").on("click",function(event){
		if (($("#picupload label.tip").length<1)&&(!/^$/i.test($("#picfile").val()))) {
			$("#myForm").submit();
		}
	});
	//$("#picupload a.btn").on("click",function(event){ $("#picfile").trigger("click"); });
	$("#picfile").on("change",function(event){
		$("#picupload label.tip").remove();
		if (/\.(jpg|png|gif|bmp)$/i.test($(this).val())) {
			var file_name = $(this).val().split(/(\\|\/)/);
			$("#subfile").val(file_name[file_name.length-1]);
		}
		else {
			$(this).val("");
			$("#subfile").val("");
			$(this).after("<label class='tip'>僅支援jpg,png,gif,bmp格式</label>");
		}
	});
});