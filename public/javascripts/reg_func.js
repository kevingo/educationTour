var ok_span = "<span class='pull-right check checkok'></span>";
var no_span = "<span class='pull-right check checkno'></span>";
function check_format(obj) {
  var target = $(obj);
  var pass = true;
  if (/^$/.test($(obj).val())) {
    pass = false;
  }
  else {
    if (/text/i.test($(obj).attr("type"))) {
      var patt = {
        email:"^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$"
      };
      for (x in patt) {
        if ($(obj).hasClass(x)) {
          var reg=new RegExp(patt[x],"i");
          if (!(reg.test($(obj).val()))) {
            pass = false;
          }
        }
      }
    }
    else if (/password/i.test($(obj).attr("type"))) {
      var value = $(obj).eq(0).val();
      $(obj).each(function(i){ pass = (value==$(this).val())?pass:false; });
    }
    else if (/radio/i.test($(obj).attr("type"))) {
      pass = false;
      $(obj).each(function(i){
        if (!pass) { pass = $(this).is(":checked"); }
      });
      target = $(obj).parents("dd").find("input:radio:last");
    }
    else if (/checkbox/i.test($(obj).attr("type"))) {
      pass = $(obj).is(":checked");
    }
    else if (/file/i.test($(obj).attr("type"))) {
      if ($(obj).hasClass("image")) { pass = /\.(jpg|png|gif|bmp)$/i.test($(obj).val()); }
      else { pass = /\.(pdf|doc|docx|jpg|png|gif|bmp)$/i.test($(obj).val()); }
    }
  }
  $(target).removeClass("error");
  if ((pass)&&($(target).parent().find("span.checkno").length<1)) {
    $(target).after(ok_span);
  }
  else {
    $(target).parent().find("span.check").remove();
    $(target).after(no_span);
    $(target).addClass("error");
  }
}