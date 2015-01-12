var map1 = null;
var marker1=null;
$(function(){
    $('.nc60').nailthumb({width:60,height:60});
    $('.nc80').nailthumb({width:80,height:80});
    $('.nc100').nailthumb({width:100,height:100});
    $('.nc120').nailthumb({width:120,height:120});
    $('.nc200').nailthumb({width:200,height:200});
    $('.nc500').nailthumb({width:500,height:500});
    $("#lesson_tab a").off("click").on("click",function(event){ event.preventDefault(); $(this).tab('show'); });
	//加入收藏
	$("button.btn-google").on("click",function(event){ msgBox("已成功加入收藏"); });
	//one-point
	one_point();
	//upload picture
    $(".upload_list a.upload_photo").off("click").on("click",function(event){
      $("#showimage h3").html($(this).find("img").attr("title"));
      $("p.showimage_info").html($(this).find("img").attr("img_info"));
      $("#showimage img").attr("src",$(this).find("img").attr("src"));
      $('#showimage').modal("toggle");
      event.stopPropagation();
    });
    $("#lesson_img_wrapper img").on("click",function(event){
      $("#showimage1 img").attr("src",$(this).attr("src"));
      $('#showimage1').modal("toggle");
      event.stopPropagation();
    });
    //picture slider
    $('#carousel').flexslider({
      animation: "slide",
      controlNav: false,
      animationLoop: false,
      slideshow: false,
      itemWidth: 103,
      itemMargin: 0,
      asNavFor: '#slider'
    });
    $('#slider').flexslider({
      animation: "slide",
      controlNav: false,
      animationLoop: false,
      slideshow: false,
      sync: "#carousel",
      start: function(slider){
        $('body').removeClass('loading');
      }
    })
});
function msgBox(msg) { $("#msg").find(".modal-body").html(msg); $("#msg").modal("toggle"); }