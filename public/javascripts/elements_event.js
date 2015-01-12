var spot_number = 0, timepicker_number = 0;
function element_event(div) {
	$(div).find("div.toolkit_item").each(function(i){
		var type_name = "";
		if ($(this).hasClass("toolkit_reference")) {
			type_name = "cite";
		}
		if ($(this).hasClass("toolkit_html")) {
			type_name = "text";
			reinit_tinyeditor();
		}
		if ($(this).hasClass("toolkit_youtube")) {
			type_name = "youtube";
		}
		if ($(this).hasClass("toolkit_pic")) {
			type_name = "photo";
			$(this).find("a.fileTrigger").off("click").on("click",function(event){ $(this).parents("div.toolkit_item").find("input:file").trigger("click"); });
			$(this).find("input:file").off("change").on("change",function(event){
				$(this).parent().find("label.tip").remove();
				if (!(/\.(jpg|png|gif|bmp)$/i.test($(this).val()))) {
					if ($(this).val()!="") {
						$(this).val("");
						$(this).after("<label class='tip'>僅支援jpg,png,gif,bmp格式</label>");
					}
				}
				var file_name = $(this).val().split(/(\\|\/)/);
				$(this).parents("div.toolkit_item").find("input.fileName").val(file_name[file_name.length-1]);
			});
		}
		if ($(this).hasClass("toolkit_file")) {
			type_name = "attatch";
			$(this).find("a.fileTrigger").off("click").on("click",function(event){ $(this).parents("div.toolkit_item").find("input:file").trigger("click"); });
			$(this).find("input:file").off("change").on("change",function(event){
				var file_name = $(this).val().split(/(\\|\/)/);
				$(this).parents("div.toolkit_item").find("input.fileName").val(file_name[file_name.length-1]);
			});
		}
		if ($(this).hasClass("toolkit_web")) {
			type_name = "link";
		}
		if ($(this).hasClass("toolkit_spot")) {
			type_name = "map";
			google_place($(this).find("input.place"));
			$(this).find("a.calc_latlng").off("click").on("click",function(event){
				var div = $(this).parents("div.toolkit_item");
				if ($(div).find(".addr").val()!="") {
					var geocoder1 = new google.maps.Geocoder();
					geocoder1.geocode({'address':$(div).find(".addr").val()},function (results,status) {
						if(status==google.maps.GeocoderStatus.OK) {
							var result = String(results[0].geometry.location);
							var arr = result.split(',');
							$(div).find(".lat").val(arr[0].replace("(",""));
							$(div).find(".lng").val(arr[1].replace(")",""));
						}
					});
				}
				event.stopPropagation();
			});
			//timepicker
			$(this).find(".timepicker").timepicker({ 'timeFormat': 'H:i', 'step': 10,'scrollDefaultNow': true });
		}
		if ($(this).hasClass("toolkit_resume")) {
			type_name = "teacher";
			resume_event();
			$(this).find("a.fileTrigger").off("click").on("click",function(event){ $(this).parents("div.toolkit_item").find("input:file").trigger("click"); });
			$(this).find("input:file").off("change").on("change",function(event){
				$(this).parent().find("label.tip").remove();
				if (!(/\.(jpg|png|gif|bmp)$/i.test($(this).val()))) {
					if ($(this).val()!="") {
						$(this).val("");
						$(this).after("<label class='tip'>僅支援jpg,png,gif,bmp格式</label>");
					}
				}
				var file_name = $(this).val().split(/(\\|\/)/);
				$(this).parents("div.toolkit_item").find("input.fileName").val(file_name[file_name.length-1]);
			});
		}
		//common event
		$(this).find("a.upup").off("click").on("click",function(event){
			var self = $(this).parents("div.toolkit_item");
			if ($(self).index()!=0) {
				var prev = $(self).prev("div.toolkit_item");
				$(self).insertBefore($(prev));
				if ($(self).find("textarea.tinyeditor").length>0) { reinit_tinyeditor(); }
				element_event($(this).parents("div.tab-pane"));
				need_to_save();
			}
		});
		$(this).find("a.downdown").off("click").on("click",function(event){
			var self = $(this).parents("div.toolkit_item");
			if ($(self).index()!=$("div.toolkit_item").length-1) {
				var nt = $(self).next("div.toolkit_item");
				$(self).insertAfter($(nt));
				if ($(self).find("textarea.tinyeditor").length>0) { reinit_tinyeditor(); }
				element_event($(this).parents("div.tab-pane"));
				need_to_save();
			}
		});
		$(this).find("a.delete").off("click").on("click",function(event){
			var obj = $(this).parents("div.toolkit_item");
			$.get("/public/javascripts/data/delete_pop.txt",function(data){
				$("body").append(data);
				$("#delete_pop").modal("toggle");
				$("#delete_pop a.done").off("click").on("click",function(event){ var parent = $(obj).parents("div.tab-pane"); $(obj).remove(); $("#delete_pop").modal("toggle"); element_event($(parent)); need_to_save(); $("#delete_pop").remove();});
			});
		});
		$(this).find("div.toolkit_number").html(i+1);
		$(this).find("input.seqno").val((i+1)+","+type_name);
	});
	if ($(div).find("div.toolkit_item").length>1) { $(div).find("a.addfiled:first").parent().show(); }
	else { $(div).find("a.addfiled:first").parent().hide(); }
}
function reinit_tinyeditor() {
	//remove cleditor
	$("textarea.tinyeditor").each(function(i){
		var editor = $(this).cleditor()[0];
		editor.$area.insertBefore(editor.$main); // Move the textarea out of the
		editor.$area.removeData("cleditor"); // Remove the cleditor pointer from the
		editor.$main.remove(); // Remove the main div and all children from the DOM
	});
	$("textarea.tinyeditor").cleditor();
	$("textarea.tinyeditor").each(function(i){ $(this).next("iframe").css("width","100%"); });
}
function resume_event(){
	$("p.dy").find("a.add_res").off("click").on("click",function(event){
		var text = '<p class="dy"><span class="tool_ele_title span2">學經歷</span><input type="text" class="span9 require" placeholder="請輸入老師學經歷" name="res_grad[]"></input><a class="btn btn-mini resume_btn add_res"><i class="icon-plus-sign-alt"></i></a><a class="btn btn-mini resume_btn del_res"><i class="icon-remove-sign"></i></a></p>';
		$(this).parents("p.dy").after(text);
		$(this).parents(".toolkit_resume").find("input.counts").val($(this).parents(".toolkit_resume").find("p.dy").length);
		resume_event();
	});
	$("p.dy").find("a.del_res").off("click").on("click",function(event){
		if ($(this).parents("div.toolkit_resume").find("p.dy").length>1){
			$(this).parents(".toolkit_resume").find("input.counts").val($(this).parents(".toolkit_resume").find("p.dy").length-1);
			$(this).parents("p.dy").remove();
		}
	});
}
function google_place(tar) {
	if ($(tar).attr("id")) { id = $(tar).attr("id"); }
	else {
		spot_number+=1;
		id = "spot"+spot_number;
		$(tar).attr("id",id);
	}
	var input = document.getElementById(id);
	var options = { componentRestrictions: {country: 'tw'} };
	var autocomplete = new google.maps.places.Autocomplete(input,options);
	google.maps.event.clearListeners(autocomplete, 'place_changed');
	google.maps.event.addListener(autocomplete, 'place_changed', function() {
	  	var place = autocomplete.getPlace();
	  	if (!place.geometry.location) {
	     	//Inform the user that the place was not found and return.
	    	alert("not found");
	    	return;
	  	}
	  	else {
	  		var block = $("#"+id).parents("div.toolkit_item");
	  		$(block).find("input.addr").val(place.formatted_address);
		  	$(block).find("input.lat").val(place.geometry.location.lat());
		  	$(block).find("input.lng").val(place.geometry.location.lng());
		  	$(block).find("input.phone").val((place.formatted_phone_number)?String(place.formatted_phone_number).replace(/\s/g,"-"):"");
	  	}	  
	}); 
}