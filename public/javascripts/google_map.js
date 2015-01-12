var map = null;
var prev_infowindow = null;
var infowindow = null;
var maker = null;
function initialize() {
    map = new google.maps.Map(document.getElementById('map_canvas'), {
        zoom: 10,
        center: new google.maps.LatLng(25.036772,121.520269), // 設定地圖中心點.
        mapTypeId: google.maps.MapTypeId.ROADMAP // HYBRID,ROADMAP,SATELLITE,TERRAIN  
    });
    $("body").append("<div id='tmp' style='display:none;'></div>");
    marker = new Array($("#tt03 div.spot").length);
    infowindow = new Array($("#tt03 div.spot").length);
    $("#tt03 div.spot").each(function(i){
        var addr = $(this).find("p.spot_address").html();
        $("#tmp").html($(this).html());
        $("#tmp").find("button").remove();
        $("#tmp").find("span").prepend("景點：");
        convert_LatLng(addr,$("#tmp").html(),i);
        $(this).find("button").off("click").on("click",function(event){
            trigger_marker(i);
            event.stopPropagation();
        });
    });
    $("#tmp").remove();
}
function addSite(siteDesc, latlng, address, marker_id) {
    marker[marker_id] = new google.maps.Marker({
        map: map,
        position : LatLng,
        title: address
    });
    infowindow[marker_id] = new google.maps.InfoWindow({
        content: siteDesc
    });
    google.maps.event.addListener(marker[marker_id], 'click', function() {
        if (prev_infowindow != null) prev_infowindow.close();
        prev_infowindow = infowindow[marker_id];
        infowindow[marker_id].open(map, marker[marker_id]);
    });
}
function trigger_marker(marker_id) {
    google.maps.event.trigger(map, 'resize');
    map.setCenter(marker[marker_id].getPosition());
    map.setZoom(10);
    google.maps.event.trigger(marker[marker_id],'click');
}
function convert_LatLng(address,description,index) {//重新定位地圖位置與標記點位置
    geocoder = new google.maps.Geocoder();
    geocoder.geocode({'address':address},function (results,status) {
        if(status==google.maps.GeocoderStatus.OK) {
            LatLng = results[0].geometry.location;
            addSite(description,LatLng,address,index);
        }
    }); 
}