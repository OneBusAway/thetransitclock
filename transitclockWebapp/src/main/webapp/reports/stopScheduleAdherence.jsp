<%@ page import="org.transitclock.utils.web.WebUtils" %>
<t:layout>
  <jsp:attribute name="title">Stop Schedule Adherence</jsp:attribute>
  <jsp:attribute name="head">
    <link href="params/reportParams.css" rel="stylesheet"/>
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>
  </jsp:attribute>
  <jsp:body>
<div style="display: none;" id="_group">stop</div>
<div style="display: none;" id="_groupId">stopId</div>

<div id="mainDiv">

<div id="title">Stop Schedule Adherence</div>

<div id="menu">

	<form id="params">
		<jsp:include page="params/fromDateNumDaysTime.jsp" />

		<div class="param">
			<label for="datatype">Data type:</label>
    		<select id="datatype" name="datatype"
    			title="Choose whether schedule adeherence will be calculated
    			using arrivals, departures, or both.">
    			<option value="">Both</option>
    			<option value="arrival">Arrivals</option>
    			<option value="departure">Departures</option>
    		</select>
		</div>

		<div class="submitDiv">
			<input type="button" id="getGroups" value="Get stop data"></input>
		</div>

	</form>

	<span id="message"></span> <br>
	<div id="extra">

		<div class="param">
			Limit stops by number of data points: <input type="number" style="width: 5em;" id="limitGroup" />
		</div>
		<div class="param">
			Get <input type="number" id="numberGroups" style="width: 5em;" value="5">
			<input type="button" id="fiveWorst" value="worst"></input> /
			<input type="button" id="fiveBest" value="best"></input> /
			<input type="button" id="fiveEarly" value="earliest"></input> /
			<input type="button" id="fiveLate" value="latest"></input>
			stops
		</div>
		<div class="param">
			Stops:
			<select multiple="multiple" id="groups" style="width:500px;"></select> <br>
		</div>

		<input type="button" id="go" value="Plot" /> <br>
	</div>

</div>

<div id="title">
	<img src="images/page-loader.gif" id="loading"></img>
</div>

</div>

<div id="boxPlot"></div>
<div id="boxPlotInfo" style="width: 700px; margin-left: auto; margin-right: auto;">
This box plot shows the distribution of schedule adherence data across stops.
For each stop, the maximum and minimum schedule adherences are horizontal grey
lines. The median, first quartile (middle value between the minimum and median),
and third quartile (middle value between the median and maximum) are horizontal
colored lines. 50% of a stop's data points lie within the colored box.
</div>

<script type="text/javascript" src="https://www.google.com/jsapi"></script>

<script type="text/javascript" src="javascript/scheduleAdherence.js"></script>
  </jsp:body>
</t:layout>
