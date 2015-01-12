var map1 = null;
var geocoder1 = new google.maps.Geocoder();
var marker1=null;
var infowindow1;
var markersArray = [];
function one_point() {
	$("#showmap").remove();
	$.get("/public/javascripts/data/onePoint.txt",function(data){
		$("body").append(data);
		map1 = new google.maps.Map(document.getElementById('map1'), {
			zoom: 10,
			center: new google.maps.LatLng(25.036772,121.520269), // 設定地圖中心點.
			mapTypeId: google.maps.MapTypeId.ROADMAP // HYBRID,ROADMAP,SATELLITE,TERRAIN  
		});
		$("button.onepoint").off("click").on("click",function(event){
			var obj = $(this).parents("div.spot");
			var desc = "";
			$("#showmap h3").html($(obj).find(".spot_title").html());
			$("#showmap h3 button").remove();
			$("#showmap .showmap_tel").html($(obj).find(".spot_tel").html());
			$("#showmap .showmap_info").html($(obj).find(".spot_intro").html());
			$("#showmap .showmap_address").html($(obj).find(".spot_address").html());
			$("#showmap .showmap_address button").remove();
			$(obj).find("p:not(:has(button))").each(function(i){desc += "<p>"+$(this).html()+"</p>"; });
			getLatLng($("#showmap .showmap_address").html(),desc);
			$('#showmap').modal("toggle");
			event.stopPropagation();
		});
		$('#showmap').on('shown', function () {
			google.maps.event.trigger(map1, 'resize');
			google.maps.event.trigger(marker1, 'click');
		});
	});
}
function clearOverlays() {
  if (markersArray) {
    for (i in markersArray) {
      markersArray[i].setMap(null);
    }
  }
}
function getLatLng(address,siteDesc) {
  clearOverlays();
  geocoder1.geocode({'address':address},function (results,status) {
      if(status==google.maps.GeocoderStatus.OK) {
          LatLng = results[0].geometry.location;
          marker1 = new google.maps.Marker({position: LatLng,map: map1});
          infowindow1 = new google.maps.InfoWindow({ content: siteDesc });
          google.maps.event.addListener(marker1, 'click', function() { infowindow1.open(map1, marker1); });
          google.maps.event.trigger(marker1,'click');
          markersArray.push(marker1);
      }
  }); 
}