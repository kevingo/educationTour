$(function(){
    //google map
    initialize();
    //tab
    $("#trip_tab a").off("click").on("click",function(event){
      event.preventDefault();
      $(this).tab('show');
      if ($(this).attr("href")=="#tt03") {
        google.maps.event.trigger(map, 'resize');
        trigger_marker(0);
      }
    });
    //upload picture
    $(".upload_list a.upload_photo").off("click").on("click",function(event){
      $("#showimage h3").html($(this).find("img").attr("title"));
      $("p.showimage_info").html($(this).find("img").attr("img_info"));
      $("#showimage img").attr("src",$(this).find("img").attr("src"));
      $('#showimage').modal("toggle");
      event.stopPropagation();
    });
    //one point in map
    one_point();
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
    });
    //picture resize in tabs
    $('.nc60').nailthumb({width:60,height:60});
    $('.nc80').nailthumb({width:80,height:80});
    $('.nc100').nailthumb({width:100,height:100});
    $('.nc120').nailthumb({width:120,height:120});
    $('.nc200').nailthumb({width:200,height:200});
    $('.nc500').nailthumb({width:500,height:500});
    //收藏
    $("button.btn-google").on("click",function(event){ msgBox("已成功加入收藏"); });
    //我要報名
    $("button.buy_btn").on("click",function(event){ $("#apply").modal("toggle"); });
    //我要報名欄位檢查
    $("button.apply").on("click",function(event){
      var obj = $(this).parents("div.apply_block");
      $(obj).find("span.check").remove();
      $(obj).find("input").each(function(i){ check_format($(this)); });
      if ($("span.checkno").length<1) { $(obj).find("form").submit(); }
    });
    $("button.cancel").on("click",function(event){$("#apply").modal("toggle");});
});
function msgBox(msg) { $("#msg").find(".modal-body").html(msg); $("#msg").modal("toggle"); }