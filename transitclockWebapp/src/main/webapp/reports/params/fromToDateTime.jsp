<%-- For specifying a begin date & time and an end date & time --%>

<script src="../javascript/jquery-timepicker/jquery.timepicker.min.js"></script>
<link rel="stylesheet" type="text/css" href="../javascript/jquery-timepicker/jquery.timepicker.css"></link>
<script src="../javascript/air-datepicker/js/datepicker.min.js"></script>
<script src="../javascript/air-datepicker/js/i18n/datepicker.en.js"></script>
<link rel="stylesheet" type="text/css" href="../javascript/air-datepicker/css/datepicker.min.css"></link>

<script>
$(function() {
  var today = new Date();

  $("#beginDate").datepicker({
    language: "en",
    dateFormat: "mm-dd-yyyy",
    maxDate: today,
    onSelect: function(_formatted, date) {
      $("#endDate").data("datepicker").update("minDate", date);
    }
  });

  $("#endDate").datepicker({
    language: "en",
    dateFormat: "mm-dd-yyyy",
    maxDate: today,
    onSelect: function(_formatted, date) {
      $("#beginDate").data("datepicker").update("maxDate", date);
    }
  });

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
    <label for="beginDate" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.bd"/></label>
    <input type="text" id="beginDate" name="beginDate"
      title="The first day of the range you want to examine data for.&#10;&#10;Begin date must be before the end date."
      value="<%= currentDateStr%>"
      class="form-control w-40"/>
  </div>

  <div>
    <label for="endDate" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.ed"/></label>
    <input type="text" id="endDate" name="endDate"
      title="The last date of the range you want to examine data for.&#10;&#10;End date must be after the begin date."
      value="<%= currentDateStr%>"
      class="form-control w-40"/>
  </div>

  <div>
    <label for="beginTime" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.bt"/></label>
    <div class="flex items-center gap-2">
      <input id="beginTime" name="beginTime"
        title="Optional begin time of day to limit query to.&#10;&#10;Format: hh:mm, as in '07:00' for 7AM."
        value=""
        class="form-control w-28"/>
      <span class="text-xs text-neutral-500">(hh:mm)</span>
    </div>
  </div>

  <div>
    <label for="endTime" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.et"/></label>
    <div class="flex items-center gap-2">
      <input id="endTime" name="endTime"
        title="Optional end time of day to limit query to.&#10;&#10;Format: hh:mm, as in '09:00' for 9AM. Use '23:59' for midnight."
        value=""
        class="form-control w-28"/>
      <span class="text-xs text-neutral-500">(hh:mm)</span>
    </div>
  </div>
