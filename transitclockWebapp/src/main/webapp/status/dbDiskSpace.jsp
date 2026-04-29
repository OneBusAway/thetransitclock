<%@ page import="org.transitclock.reports.DbDiskSpaceQuery" %>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}

pageContext.setAttribute("jsonData1", DbDiskSpaceQuery.getTotalsJson(agencyId));
pageContext.setAttribute("jsonData2", DbDiskSpaceQuery.getDetailsJson(agencyId));
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.ddsu" /></jsp:attribute>
  <jsp:attribute name="head">
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      // Load in Google charts library
      google.load("visualization", "1", {packages:["table", "corechart"]});

      // When charts library ready then actually draw the tables/charts
      google.setOnLoadCallback(drawCharts);

      // Sets the property for the entire column, except for the header.
      // Need to use this for Table since it ignores column wide properties.
      // Can be used to set propertyName of 'style' or 'className'.
      function setColumnProperty(dataTable, columnIndex, propertyName, propertyValue) {
    	  for (var rowIndex=0; rowIndex<dataTable.getNumberOfRows(); ++rowIndex) {
    		  dataTable.setProperty(rowIndex, columnIndex, propertyName, propertyValue);
    	  }
      }

      // Sets the property for the entire row.
      // Need to use this for Table since it ignores row wide properties.
      // Can be used to set propertyName of 'style' or 'className'.
      function setRowProperty(dataTable, rowIndex, propertyName, propertyValue) {
    	  for (var columnIndex=0; columnIndex<dataTable.getNumberOfColumns(); ++columnIndex) {
    		  dataTable.setProperty(rowIndex, columnIndex, propertyName, propertyValue);
    	  }
      }

      // To be called at page load. Renders the tables and charts
      function drawCharts() {
    	var jsonData = ${jsonData1};
        var data1 = new google.visualization.DataTable(jsonData);
        // Make total size cells right justified. When setting to class
        // totalSizeCell also need to set to google-visualization-table-td
        // because otherwise the default properties for those cells are erased.
        setColumnProperty(data1, 1, 'className', 'totalSizeCell google-visualization-table-td');

        // Make the total bold so it stands out
        var FOO = data1.getNumberOfRows();
        setRowProperty(data1, data1.getNumberOfRows()-1, 'style', 'font-weight: bold;');
        data1.setProperty(data1.getNumberOfRows()-1, 0, 'style', 'text-align: right; font-weight: bold;');

        // Use special formatter for the bytes column, column #2, to add commas
        // between thousands
        var formatter = new google.visualization.NumberFormat({pattern:'#,###'});
        formatter.format(data1, 2);

        // Create the bar chart view
        var chartView = new google.visualization.DataView(data1);
        chartView.setColumns([0,2]);
        // Only show data for biggest db tables
        chartView.setRows(0,7);

        // Create the actual bar chart
        var chart = new google.visualization.BarChart(document.getElementById("chart_div"));
        var chartOptions = {
        	legend: {position: 'none'},
        	// Take up most of the space with the actual chart
        	chartArea: {left: '20%', width: '80%', top: 0, height: '90%'},
        	// 2/23/2015 Unfortunately startup animation not working. Get
        	// message "g is undefined" from Google library
        	// I believe this problem is temporary and will be
        	// fixed by Google at some point.
        	//animation: {duration: 1500, startup: true },
        };
        chart.draw(chartView , chartOptions);


        // Create a view for table chart so can eliminate unneeded columns and do sorting
        // properly even with the Totals: row at bottom.
        var view1 = new google.visualization.DataView(data1);
        view1.setColumns([0,1,2]);

        // Actually create and draw the table
        var table1 = new google.visualization.Table(document.getElementById('table1_div'));
        var table1Options = {showRowNumber: false, allowHtml: true, sort: 'event'};
        table1.draw(view1, table1Options);

        // Create second table that also breaks out sizes of indices and keys
        // for tables
    	var jsonData2 = ${jsonData2};
        var data2 = new google.visualization.DataTable(jsonData2);

        // Make total size cells right justified. When setting to class
        // totalSizeCell also need to set to google-visualization-table-td
        // because otherwise the default properties for those cells are erased.
        setColumnProperty(data2, 1, 'className', 'totalSizeCell google-visualization-table-td');

        // Use special formatter for the bytes column, column #2, to add commas
        // between thousands
        formatter.format(data2, 2);

        // Actually create and draw the table
        var table2 = new google.visualization.Table(document.getElementById('table2_div'));
        table2.draw(data2, {showRowNumber: false, allowHtml: true});

        // Kludge to get the Google Charts Table to only take up as much
        // width as actually needed. By doing this the table can be centered
        // in the page without having to set it to an arbitrary width.
        // This works by removing the width of the actually html tables
        // and then setting margin-left and margin-right to auto to actually
        // center the table. Using an inline function so can pass in additional
        // parameters to the function that actually does the sorting.
        google.visualization.events.addListener(table1, 'sort', function(sortProperties) {
        	sortWithTotalOnBottom(sortProperties, table1, table1Options, view1)
        });
        google.visualization.events.addListener(table2, 'sort', sortWidthHandler);
        sortWidthHandler();
      }

      // For sorting table with Total: row at bottom. Makes sure that row
      // stays at the bottom
      function sortWithTotalOnBottom(sortProperties, table, tableOptions, view) {
    	  var sortedRows = view.getSortedRows({column: sortProperties.column,
    		                                   desc: !sortProperties.ascending});

    	  // Find index of where the Totals row is
    	  for (var i=0; i<sortedRows.length; ++i) {
    		  // If found the last Totals row...
    		  if (sortedRows[i] == sortedRows.length-1) {
    			  // Move the Total row to the end by moving the indixes around
    			  sortedRows.splice(i, 1);
    			  sortedRows.push(sortedRows.length);
    			  break;
    		  }
    	  }

    	  // Actually update the table with the proper order. First had
    	  // tried just updating sortProperties.sortedIndexes and that almost
    	  // worked but often the Totals row was placed in the middle of the table.
    	  // Note that also need to reset the table options. Otherwise seem
    	  // to lose ability to use HTML in cells.
    	  view.setRows(sortedRows);
    	  table.draw(view, tableOptions);

    	  // Get the width to be correct. Otherwise after sorting the table will
    	  // take up all width available.
    	  sortWidthHandler();
      }

      // Kludge to get the Google Charts Table to only take up as much
      // width as actually needed.
      function sortWidthHandler() {
          $(".google-visualization-table-table").css("width", "");
          $(".google-visualization-table-table").css("margin-left", "auto");
          $(".google-visualization-table-table").css("margin-right", "auto");
          $(".google-visualization-table").css("width", "");
      }
    </script>

  <style>

  	.totalSizeCell {
  		text-align: right;
  	}

  	#chart_div {
  		width: 80%;
  		height: 250px;
  		margin-left: auto;
  		margin-right: auto;
  		margin-bottom: 30px;
  	}

  	/* For centering google chart tables horizontally */
  	.google-visualization-table {
  		width: 100%;
  	}

  </style>
  </jsp:attribute>
  <jsp:body>
<div id="title"><fmt:message key="div.ddsflt" /></div>
<div id="chart_div"></div>

<div id="title"><fmt:message key="div.ddsbt" /></div>
<div id="table1_div"></div>

<div id="title"><fmt:message key="div.ddsd" /></div>
<div id="table2_div"></div>
  </jsp:body>
</t:layout>
