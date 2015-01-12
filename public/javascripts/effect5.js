var element_limit = 3;
var url1 = "/ajax/tagCategory?callback=get_ref_menu";
var url2 = "/ajax/getListTPByTag";
var url3 = "/ajax/getTeachingPlanInfo";
var end0=false, end1=false, end2=false, end3=false;
$(function(){
	$("#add_elements").on("click",function(event){
		 var obj = $(this).parent();
		 if ($(obj).find("div.element").length<element_limit) { $("#element_pop").modal("toggle"); }
		 else { alert("您可選擇的標籤已達上限"); }
		});
	$('#create_lesson_tab a').click(function (e) {e.preventDefault();$(this).tab('show');});
	//reg
	$("a.save").on("click",function(event){
		$("span.check").remove();
		var pass=new Array();
		//form1
		check_format($("#lesson_name"));
		//check_format($("#lesson_subject"));
		//check_format($("#lesson_unit"));
		$("#add_elements").after($("div.element").length>0?ok_span:no_span);
		pass.push(($("#basic span.checkno").length<1)?true:false);
		//form2~form4
		$("#prelearn,#middlelearn,#afterlearn").each(function(i){
			$(this).find("input.require").each(function(j){ check_format($(this)); });
			pass.push(($(this).find("span.checkno").length<1)?true:false);
		});
		if (pass[0]&pass[1]&pass[2]&pass[3]) {
			before_off_unload();
			$("body").append('<div id="status">Saving....Please Wait</div><div class="modal-backdrop fade in" id="processing"></div>');
			$("#basic_ifm,#prelearn_ifm,#middlelearn_ifm,#afterlearn_ifm").remove();
			$("body").append("<iframe id='basic_ifm' name='basic_ifm'></iframe><iframe id='prelearn_ifm' name='prelearn_ifm'></iframe><iframe id='middlelearn_ifm' name='middlelearn_ifm'></iframe><iframe id='afterlearn_ifm' name='afterlearn_ifm'></iframe>");
			$("#basicFM").submit();
			$("#basic_ifm").load(function(){
				$("#prelearnFM").submit();
				$("#prelearn_ifm").load(function(){
					$("#middlelearnFM").submit();
					$("#middlelearn_ifm").load(function(){
						$("#afterlearnFM").submit();
						$("#afterlearn_ifm").load(function(){
							setTimeout(function(){
								$("#status,#processing").remove();
								$("#basic_ifm,#prelearn_ifm,#middlelearn_ifm,#afterlearn_ifm").remove();
							},2000);
						});
					});
				});
			});
		}
		else {
			alert("請確認所有必填欄位都填寫完畢");
			var tar = $("span.checkno:first").parents("div.tab-pane");
			$("a[href='#"+$(tar).attr("id")+"']").trigger("click");
		}
		event.stopPropagation();
	});
	//next tab
	$("a.next").on("click",function(event){$("#create_lesson_tab li.active").next("li").find("a").trigger("click");});
	//prev tab
	$("a.prev").on("click",function(event){$("#create_lesson_tab li.active").prev("li").find("a").trigger("click");});
	//add element button in tab2~tab4
	$("a.addfiled").on("click",function(event){$("#ref_cancel").trigger("click");$("#add_element").modal("toggle");});
	/* Reference - start */
	//top content function
	$.getJSON(url1,function(json){ get_ref_menu(json); });
	//get_ref_menu({"response":{"result":"true","categorys":[{"tags":[{"id":11,"name":"北部地區"},{"id":12,"name":"中部地區"},{"id":13,"name":"南部地區"}],"id":6,"name":"地區"},{"tags":[{"id":16,"name":"音樂"},{"id":14,"name":"歷史"},{"id":15,"name":"美術"}],"id":7,"name":"科目"},{"tags":[{"id":17,"name":"春"},{"id":19,"name":"秋"},{"id":18,"name":"夏"},{"id":20,"name":"冬"}],"id":8,"name":"時間"}]}});//暫時先用假的
	//add function
	$("#ref_add").on("click",function(event){
		if ($("dd.active").length>0) {
			var obj = $("dd.active");
			var title = $(obj).find("a").html();
			var creator = $(obj).parents("li").find(".lesson_teacher").html();
			var name_chapter = $(obj).parents("li").find(".lesson_title").html();
			var type = $(obj).find("a").attr("lstye");
			var id1 = $(obj).find("a").attr("lid");
			var id2 = $(obj).find("a").attr("tpid");
			create_lesson("addeleli01",{lesson_title:title,lesson_creator:creator,lesson_name_chapter:name_chapter,toolkit_type:type,style:type,id:id1,tpid:id2});
		}
	})
	//cancel function
	$("#ref_cancel").on("click",function(event){
		$("#add_element li").removeClass("active");
		$("#add_element div.main,#add_element div.modal-footer a:not(.ref)").show();
		$("#add_element div.ref,#add_element div.modal-footer a.ref").hide();
	});
	/* Reference - end */
	//#add_element pop - new button
	$("#add_element li").on("click",function(event){
		$("#add_element li").removeClass("active");
		$(this).addClass("active");
		if ($(this).hasClass("addeleli01")) {
			$("#add_element div.main,#add_element div.modal-footer a:not(.ref)").hide();
			$("#add_element div.ref,#add_element div.modal-footer a.ref").show();
			$("div.ref .lessons").html("");
		}
	});
	$("#create_ele").on("click",function(event){
		var classes = $("#add_element li.active").attr("class");
		var class_name = classes.match(/addeleli\d{2}/i);
		create_lesson(class_name);
	});
	//elements pop
	$("#element_pop li").on("click",function(event){
		if ($("div.element input:hidden[value='"+$(this).attr("eid")+"']").length<1) {
		  $("#add_elements").before("<div class='element'>"+$(this).html()+"<span>x</span><input type='hidden' name='element[]' value='"+$(this).attr("eid")+"'></div>");
		  $("div.element span").off("click").on("click",function(event){ $(this).parent().remove(); event.stopPropagation(); });
		  unload_event();
		  $("#element_pop").modal("toggle");
		 }
		else
			alert("不可選取相同的標籤");
	});
	/*edit function*/
	/*if (/\/tp\/create\/\d+$/.test(window.location.href)) {
		var value = window.location.href.split("/");
		$("#tpID").val(value[value.length-1]);
		load_data();
	}*/
	load_data();
	before_off_unload();
});
function unload_event() {
	before_off_unload();
	before_unload();
}
function before_unload() {
	$(window).off("beforeunload").on("beforeunload",function(event){ return "你確定要離開此頁面嗎？(不儲存離開)"; });
	$("a.save").show();
}
function before_off_unload() {
	$(window).off("beforeunload");
	//$("a.save").hide();
	$("input:text,textarea").off("keyup",need_to_save).one("keyup",need_to_save);
	$("select,input:file").off("change",need_to_save).one("change",need_to_save);
}
function need_to_save() {
	before_unload();
	$("input:text,textarea").off("keyup",need_to_save);
	$("select,input:file").off("change",need_to_save);
}
function get_ref_menu(json) {
	if (json) {
		if (json.response.result) {
			var cate = json.response.categorys;
			for (var i=0;i<cate.length;i++) {
				var mclass = "menu"+cate[i].id;
				$("#menu").append("<option value='"+cate[i].id+"'>"+cate[i].name+"</option>");
				$("div.menu"+cate[i].id).remove();
				$("#menu").after("<p class='add_other_lesson_filter "+mclass+"' style='display:none;'></p>");
				for (var j=0;j<cate[i].tags.length;j++) {
					$("p."+mclass).append("<span><a cid='"+cate[i].tags[j].id+"'>"+cate[i].tags[j].name+"</a></span>");
				}
			}
		}
		$("#menu").append("<option value='mytp'>我的教案</option>");
		$("#menu").append("<option value='myfavor'>我的收藏教案</option>");
		$("#menu").on("change",function(event){
			var obj = $(this).parent();
			$(obj).children("p").hide();
			if (/^myfavor$/.test($(this).val())) {
				$.post(url2,{tagId:0},function(json){ get_ref_list(json); },"json");
			}
			else if(/^mytp$/.test($(this).val())) {
				$.post(url2,{tagId:-1},function(json){ get_ref_list(json); },"json");
			}
			else {$(obj).find("p.menu"+$(this).val()).show();}
		});
		$("#menu").parent().find("a").on("click",function(event){
			$.post(url2,{tagId:$(this).attr("cid")},function(json){ get_ref_list(json); },"json");
			//get_ref_list({response:{result:true,TeachingPlans:[{id:1,name:"漢字文化",creator:"1234",before:[{id:1,style:"map",name:"地圖1"},{id:2,style:"youtube",name:"Youtubd1"}],now:[{id:1,style:"map",name:"地圖1-中期"},{id:2,style:"youtube",name:"Youtubd1"}],after:[{id:1,style:"map",name:"地圖1-後期"},{id:2,style:"youtube",name:"Youtubd1"}]}]}});
		});
		$("#menu").trigger("change");
	}
}
function get_ref_list(json) {
	if (json) {
		if (json.response.result) {
			var plans = json.response.TeachingPlans;
			$("ul.lessons").html("");
			for (var i=0;i<plans.length;i++) {
				var chapter = "";			
				chapter += get_chapter_list("學前課程",plans[i].before,plans[i].id);
				chapter += get_chapter_list("學中課程",plans[i].now,plans[i].id);
				chapter += get_chapter_list("學後課程",plans[i].after,plans[i].id);
				chapter = "<dl class='lesson_chapter'>"+chapter+"</dl>";
				$("ul.lessons").append("<li><span class='lesson_title'>"+plans[i].name+"</span><span class='pull-right lesson_teacher'>"+plans[i].creator+"老師</span>"+chapter+"</li>");
			}
			$("ul.lessons a[lid]").off("click").on("click",function(event){$(this).parents("dl.lesson_chapter").find("dd").removeClass("active");$(this).parent().addClass("active");});
		}
	}
}
function get_chapter_list(title_name, json_data, tpid) {
	var str = "";	
	if (json_data) {
		str = "<dt>"+title_name+"</dt>";
		for (var i=0;i<json_data.length;i++) { 
			if(json_data[i].id!=null)
				str += "<dd><a tpid='"+tpid+"' lid='"+json_data[i].id+"' lstye='"+json_data[i].style+"'>"+json_data[i].name+"</a></dd>"; 
		}
	}
	return str;
}
function create_lesson(class_name,json) {
	var block = $($("#create_lesson_tab>li.active").find("a").attr("href"));
	$.get("/public/javascripts/data/"+class_name+".txt",function(data){
		$(block).find("div.add_tools").append(data);
		element_event($(block));
		if (json) {
			for (x in json) {
				var obj = $(block).find("div.toolkit_item:last ."+x);
				if ($(obj).attr("type")) { $(obj).val(json[x]); }
				else { $(obj).html(json[x]); }
			}
		}
		$("#add_element").modal("toggle");
	});
	unload_event();
}
function load_data() {
	$.post(url3,{tpId: $("#tpID").val()},function(json){ assign_data(json); },"json");
}
function assign_data(json) {
	if (json) {
		var data = json.response;
		//basic data
		$("#tpID").val(data.id);
		$("input[name='lesson_name']").val(data.name);
		$("select[name='publish'] option[value='"+((data.publish)?"1":"0")+"']").attr("selected",true);
		$("select[name='share'] option[value='"+((data.share)?"1":"0")+"']").attr("selected",true);
		for (var i=0;i<data.tags.length;i++) {
			$("#add_elements").before("<div class='element'>"+data.tags[i].name+"<span>x</span><input type='hidden' name='element[]' value='"+data.tags[i].id+"'></div>");
		}
		$("div.element span").off("click").on("click",function(event){$(this).parent().remove();});
		$("#basic textarea").val(data.introduction);
		//before
		$("#prelearn div.basic_info textarea").val(data.before.illustration);
		writedown_elements($("#prelearn"),data.before.components,0);
		//now
		$("#middlelearn div.basic_info textarea").val(data.now.illustration);
		writedown_elements($("#middlelearn"),data.now.components,0);
		//after
		$("#afterlearn div.basic_info textarea").val(data.after.illustration);
		writedown_elements($("#afterlearn"),data.after.components,0);
	}
}
function writedown_elements(block, arr, index) {
	if (arr) {
		if (index<arr.length) {
			var types = {cite: "01", text: "02", photo: "03", youtube: "04", attatch: "05", link:"06", map:"07", teacher:"08"};
			var style = types[arr[index].style];
			if(style!=null){
				$.get("/public/javascripts/data/addeleli"+style+".txt",function(data){
					$(block).find("div.add_tools").append(data);
					var obj = $(block).find("div.toolkit_item:last");
					var json = null;
					if (/cite/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							lesson_title: arr[index].citeTitle,
							id: arr[index].citeId,
							tpid: arr[index].citeTpId,
							tpname:arr[index].citeTpName,
							style: arr[index].ref_style,
							memo: arr[index].intro
						};
						$(obj).find("span.toolkit_type").html(json.style);
						$(obj).find(".lesson_creator").html(arr[index].citeTeacher);
						$(obj).find(".lesson_name_chapter").html(arr[index].citeTpName);
					}
					if (/text/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							tinyeditor: arr[index].intro,
							share: (arr[index].share)?"yes":"no"
						};
					}
					if (/photo/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							fileName: arr[index].fileName,
							share: (arr[index].share)?"yes":"no"
						};
					}
					if (/youtube/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							url: arr[index].url,
							share: (arr[index].share)?"yes":"no"
						};
					}
					if (/attatch/.test(arr[index].style)) {	
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							fileName: arr[index].fileName,
							share: (arr[index].share)?"yes":"no"
						}
					}
					if (/link/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							webName: arr[index].linkName, 
							webAddr: arr[index].url,
							share: (arr[index].share)?"yes":"no"
						}
					}
					if (/map/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							share: (arr[index].share)?"yes":"no",
							addr: arr[index].address,
							phone: arr[index].tel,
							op: arr[index].openTime,
							cl: arr[index].endTime,
							lat: arr[index].lat,
							lng: arr[index].lng,
							place: arr[index].POIName
						};
					}
					if (/teacher/.test(arr[index].style)) {
						json = {
							ele_id: arr[index].id,
							title: arr[index].title,
							memo: arr[index].intro,
							share: (arr[index].share)?"yes":"no",
							tname: arr[index].teacherName,
							fileName: (arr[index].photo!=null)?arr[index].photo.fileName:null,
							edu: arr[index].education.split("##")
						}
						var p = $(obj).find("p.dy:first").html();
						for (var z = 0;z<json.edu.length-2;z++) { $(obj).find("p.dy:first").after("<p class='dy'>"+p+"</p>"); }
						for (var z=0;z<json.edu.length-1;z++) {$(obj).find("p.dy:eq("+z+") input").val(json.edu[z]);}
						$(obj).find("input.counts").val(json.edu.length-1);
					}
					if (json) { for (x in json) { $(obj).find("."+x).val(json[x]); } }
					writedown_elements($(block), arr, index+1);
				});
			}
		}
		else {
			element_event($(block));
			before_off_unload();
			//reinit_tinyeditor();
		}
	}
}