<%-- Paired allowable-early / allowable-late inputs in minutes. --%>
<div>
  <label for="allowableEarly" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.aear"/></label>
  <div class="flex items-center gap-2">
    <input id="allowableEarly" name="allowableEarly" type="number" value="1.0" step="0.1"
      title="How early a vehicle can arrive compared to the prediction and still be acceptable."
      class="block w-24 rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
    <span class="text-xs text-gray-500"><fmt:message key="div.minutes"/></span>
  </div>
</div>

<div>
  <label for="allowableLate" class="block text-sm font-medium text-gray-900 mb-1"><fmt:message key="div.alat"/></label>
  <div class="flex items-center gap-2">
    <input id="allowableLate" name="allowableLate" type="number" value="4.0" step="0.1"
      title="How late a vehicle can arrive compared to the prediction and still be acceptable."
      class="block w-24 rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
    <span class="text-xs text-gray-500"><fmt:message key="div.minutes"/></span>
  </div>
</div>
