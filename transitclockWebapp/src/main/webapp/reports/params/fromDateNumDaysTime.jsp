<%-- For specifying a begin date, number of days, begin time, and end time --%>

<script src="../javascript/jquery-timepicker/jquery.timepicker.min.js"></script>
<link rel="stylesheet" type="text/css" href="../javascript/jquery-timepicker/jquery.timepicker.css"></link>

<script>
$(function() {
  var calendarIconTooltip = "Popup calendar to select date";

  $( "#beginDate" ).datepicker({
	dateFormat: "mm-dd-yy",
    showOtherMonths: true,
    selectOtherMonths: true,
    buttonImage: "images/calendar.png",
    buttonImageOnly: true,
    showOn: "both",
    maxDate: 0,
    onClose: function( selectedDate ) {
      // FIXME $(".ui-datepicker-trigger").attr("title", calendarIconTooltip);
    }
  });

  $(".ui-datepicker-trigger").attr("title", calendarIconTooltip);

  $("#beginTime, #endTime").timepicker({timeFormat: "H:i"})
  	.on('change', function(evt) {
	  if (evt.originalEvent) {
	  	if (!evt.target.value.match(/^(([0,1][0-9])|(2[0-3])):[0-5][0-9]$/))
	  		evt.target.value = evt.target.oldval ? evt.target.oldval : "";
	  }
	  evt.target.oldval = evt.target.value;
  	});
});
</script>

<%
String currentDateStr = org.transitclock.utils.Time.dateStr(new java.util.Date());
%>

  <div>
    <label for="beginDate" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.bd"/></label>
    <input type="text" id="beginDate" name="beginDate"
      title="The first day of the range you want to examine data for.<br><br>Begin date must be before the end date."
      value="<%= currentDateStr%>"
      class="block w-40 rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
  </div>

  <div>
    <label for="numDays" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.nod"/></label>
    <select id="numDays" name="numDays"
      title="The number of days you want to examine data for."
      class="block w-32 rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6">
      <c:forEach var="i" begin="1" end="31">
        <option value="${i}">${i}</option>
      </c:forEach>
    </select>
  </div>

  <div>
    <label for="beginTime" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.bt"/></label>
    <div class="flex items-center gap-2">
      <input id="beginTime" name="beginTime"
        title="Optional begin time of day to limit query to. Useful if want to see result just for rush hour, for example. Leave blank if want data for entire day.<br/><br/>Format: hh:mm, as in '07:00' for 7AM."
        value=""
        class="block w-28 rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
      <span class="text-xs text-gray-500">(hh:mm)</span>
    </div>
  </div>

  <div>
    <label for="endTime" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.et"/></label>
    <div class="flex items-center gap-2">
      <input id="endTime" name="endTime"
        title="Optional end time of day to limit query to. Useful if want to see result just for rush hour, for example. Leave blank if want data for entire day.<br/><br/>Format: hh:mm, as in '09:00' for 9AM. Use '23:59' for midnight."
        value=""
        class="block w-28 rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
      <span class="text-xs text-gray-500">(hh:mm)</span>
    </div>
  </div>
