<%-- Prediction-type filter (All / Affected by layover / Not affected). --%>
<div>
  <label for="predictionType" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.ptype"/></label>
  <select id="predictionType" name="predictionType"
    title="Specifies whether or not to show prediction accuracy for predictions that were affected by a layover. Select 'All' to show data for predictions, 'Affected by layover' to only see data where predictions affected by when a driver is scheduled to leave a layover, or 'Not affected by layover' if you only want data for predictions that were not affected by layovers."
    class="block w-full max-w-md rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6">
    <option value=""><fmt:message key="div.pall"/></option>
    <option value="AffectedByWaitStop"><fmt:message key="div.paff"/></option>
    <option value="NotAffectedByWaitStop"><fmt:message key="div.pnaff"/></option>
  </select>
</div>
