<%-- This is an experimental page for using a smartphone as a tracker. --%>
<t:layout>
  <jsp:attribute name="title">Tracking</jsp:attribute>
  <jsp:body>
<div id="output">Tracking...</div>

<script>

var positions=[];

function storePosition(position) {
	// Make a copy since the position variable passed in
	// is reused.
	var positionsCopy = $.extend( {}, position);

	// If a new position (indicated by new timestamp) then
	// add to array of positions
	if (positions.length == 0
			|| position.timestamp != positions[positions.length-1].timestamp)
		positions.push(positionsCopy);

	// Write only when get 10 data points so that don't
	// try to communicate every second
	if (positions.length >= 10) {
		var dataStr = '';
		for (var i=0; i<positions.length; ++i) {
			var p = positions[i];
			dataStr += p.coords.latitude + ','
				+ p.coords.longitude + ','
				+ p.coords.speed + ','
				+ p.coords.heading + ','
				+ p.coords.accuracy + ','
				+ p.timestamp + '\n';
		}
		//alert("Sending:\n" + dataStr);
		// Send dataStr to web server for it to store the data
	    $.ajax({
	    	type: "POST",
	    	url: "./tracker/store.jsp",
	    	data: dataStr,
	    	success: function(data) {
	    		//alert(data);
	    	}
	    });

	    // Clear array
	    positions.length = 0;
	}
}

function handlePosition(position) {
    $("#output").html("Latitude: " + position.coords.latitude
      + "<br>Longitude: " + position.coords.longitude
      + "<br>Speed:" + position.coords.speed
      + "<br>Heading:" + position.coords.heading
      + "<br>timestamp:" + position.timestamp);

	storePosition(position);

	// Done getting position
	gettingLocation = false;
}

function showError(error) {
    switch(error.code) {
        case error.PERMISSION_DENIED:
        	$("#output").html("User denied the request for Geolocation.");
            break;
        case error.POSITION_UNAVAILABLE:
        	$("#output").html("Location information is unavailable.");
    	    $.ajax({
    	    	type: "POST",
    	    	url: "./tracker/store.jsp",
    	    	data: "--Location information is unavailable.",
    	    });

            break;
        case error.TIMEOUT:
        	$("#output").html("The request to get user location timed out.");
            break;
        case error.UNKNOWN_ERROR:
        	$("#output").html("An unknown error occurred.");
            break;
    }
}

//For making sure that only getting position once at a time
var gettingLocation = false;

function getLocation() {
    if (navigator.geolocation) {
    	gettingLocation = true;
        navigator.geolocation.getCurrentPosition(handlePosition, showError);
    } else {
    	$("#output").html("Geolocation is not supported by this browser.");
    }
}

var timeOfLastGetLocation = 0;

function interval() {
	var currentTime = (new Date()).getTime();
	if (currentTime-timeOfLastGetLocation > 1000 && !gettingLocation) {
		timeOfLastGetLocation = currentTime;
		getLocation();
    }
}

// Try to read in location every second
setInterval(interval, 100);

</script>
  </jsp:body>
</t:layout>
