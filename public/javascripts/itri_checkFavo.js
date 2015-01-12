function addFavo(email, id, type) {
	jQuery.ajax({
        type: 'PUT',
        url: "/ajax/addToFavorite",                
        data: {
        	email:email,
        	id:id,
        	type:type
     	},
        success: function() {
        	window.location.reload();
        },
        error: function(a,b,c) {
            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
        }
    });
}

function removeFavo(email, id, type) {
	jQuery.ajax({
        type: 'PUT',
        url: "/ajax/removeToFavorite",                
        data: {
        	email:email,
        	id:id,
        	type:type
     	},
        success: function() {
        	window.location.reload();
        },
        error: function(a,b,c) {
            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
        }
    });
}

/* For tp, oa show page to manage enable or disable */
function enableHandler(action, type, id) {
	if(type=='tp') {		
		if(action=='disable') {
			if(confirm("是否確認要下架本教案")) {
				jQuery.ajax({
			        type: 'PUT',
			        url: "/tp/disable",                
			        data: {
			        	id:id
			     	},
			        success: function() {
			        	window.location.reload();
			        },
			        error: function(a,b,c) {
			            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
			        }
			    });
			}
		} 
		else if(action=='enable') {
			if(confirm("是否確認要上架本教案")) {
				jQuery.ajax({
			        type: 'PUT',
			        url: "/tp/enable",                
			        data: {
			        	id:id
			     	},
			        success: function() {
			        	window.location.reload();
			        },
			        error: function(a,b,c) {
			            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
			        }
			    });
			}
		}
	}
	else if(type=='oa') {
		if(action=='disable') {
			if(confirm("是否確認要下架本活動")) {
				jQuery.ajax({
			        type: 'PUT',
			        url: "/oa/disable",                
			        data: {
			        	id:id
			     	},
			        success: function() {
			        	window.location.reload();
			        },
			        error: function(a,b,c) {
			            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
			        }
			    });
			}
		}
		else if(action=='enable') {
			if(confirm("是否確認要上架本活動")) {
				jQuery.ajax({
			        type: 'PUT',
			        url: "/oa/enable",                
			        data: {
			        	id:id
			     	},
			        success: function() {
			        	window.location.reload();
			        },
			        error: function(a,b,c) {
			            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
			        }
			    });
			}
		}
		
	}
}

function del(type, id) {
	if(type=='tp') {	
		if(confirm("是否確認要刪除本教案")) {
			jQuery.ajax({
		        type: 'PUT',
		        url: "/tp/delete",                
		        data: {
		        	id:id
		     	},
		        success: function() {
		        	window.location.href= '/';
		        },
		        error: function(a,b,c) {
		            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
		        }
		    });
		}
	} 
	else if(type='oa') {
		if(confirm("是否確認要刪除本活動")) {
			jQuery.ajax({
		        type: 'PUT',
		        url: "/oa/delete",                
		        data: {
		        	id:id
		     	},
		        success: function() {
		        	window.location.href= '/';
		        },
		        error: function(a,b,c) {
		            alert("XMLHttpRequest : " + a + " textStatus : " + b + " errorThrown : " + c);
		        }
		    });
		}
	}
		
}

function goToTag(t_id, type) {
	if(type=='oa')
		window.location.href = '/searchOA/' + t_id;
	else if(type='tp')
		window.location.href = '/searchTP/' + t_id;
}
