function google_place(obj_id) {
	var input = document.getElementById(obj_id);
	var options = { componentRestrictions: {country: 'tw'} };
	var autocomplete = new google.maps.places.Autocomplete(input,options);
	google.maps.event.addListener(autocomplete, 'place_changed', function() {
	  	var place = autocomplete.getPlace();
	  	if (!place.geometry.location) {
	     	//Inform the user that the place was not found and return.
	    	alert("not found");
	    	return;
	  	}
	  	else {
	  		var block = $("#"+obj_id).parents("div.toolkit_item");
	  		$(block).find("input.addr").val(place.formatted_address);
		  	$(block).find("input.lat").val(place.geometry.location.lat());
		  	$(block).find("input.lng").val(place.geometry.location.lng());
		  	$(block).find("input.phone").val((place.formatted_phone_number)?String(place.formatted_phone_number).replace(/\s/g,"-"):"");
	  	}	  
	}); 
}