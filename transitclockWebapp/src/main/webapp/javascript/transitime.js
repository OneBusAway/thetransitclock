/**
 * This file contains general Javascript utilities for the Transitime system.
 * It is expected that this file will be loaded for many web pages.
 */

/**
 * For getting parameters from query string
 */
function getQueryVariable(paramName) {
	var query = window.location.search.substring(1);
    var vars = query.split("&");
    for (var i=0;i<vars.length;i++) {
    	var pair = vars[i].split("=");
        if (pair[0] == paramName){
        	return pair[1];
        }
    }

    // Didn't find the specified param so return false
    return false;
}
