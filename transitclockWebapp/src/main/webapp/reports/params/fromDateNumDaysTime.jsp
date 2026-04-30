<%-- For specifying a begin date, number of days, begin time, and end time --%>

<script src="../javascript/air-datepicker/js/datepicker.min.js"></script>
<script src="../javascript/air-datepicker/js/i18n/datepicker.en.js"></script>
<link rel="stylesheet" type="text/css" href="../javascript/air-datepicker/css/datepicker.min.css"></link>

<script>
$(function() {
  $("#beginDate").datepicker({
    language: "en",
    dateFormat: "mm-dd-yyyy",
    maxDate: new Date()
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
    <label for="numDays" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.nod"/></label>
    <select id="numDays" name="numDays"
      title="The number of days you want to examine data for."
      class="form-control w-32">
      <c:forEach var="i" begin="1" end="31">
        <option value="${i}">${i}</option>
      </c:forEach>
    </select>
  </div>

  <div>
    <label for="beginTime" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.bt"/></label>
    <input type="time" id="beginTime" name="beginTime" step="60"
      title="Optional begin time of day to limit query to. Useful if want to see result just for rush hour, for example. Leave blank if want data for entire day."
      value=""
      class="form-control w-32"/>
  </div>

  <div>
    <label for="endTime" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.et"/></label>
    <input type="time" id="endTime" name="endTime" step="60"
      title="Optional end time of day to limit query to. Useful if want to see result just for rush hour, for example. Leave blank if want data for entire day.&#10;&#10;To include the full day, use '23:59'."
      value=""
      class="form-control w-32"/>
  </div>
