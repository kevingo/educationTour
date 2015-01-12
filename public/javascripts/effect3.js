$(function(){
  $("#loginBtn").off("click").on("click",function(event){
    event.preventDefault();
    $("label.warning").remove();
    var passed = true;
    $("#logFM").find("input").each(function(i){ passed = (passed)?reg_check($(this)):passed; });
    if (passed) {
      $("#logFM").submit();
    }
  });
});
