<%-- Paired allowable-early / allowable-late inputs in minutes. --%>
<div>
  <label for="allowableEarly" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.aear"/></label>
  <div class="flex items-center gap-2">
    <input id="allowableEarly" name="allowableEarly" type="number" value="1.0" step="0.1"
      title="How early a vehicle can arrive compared to the prediction and still be acceptable."
      class="form-control w-24"/>
    <span class="text-xs text-neutral-500"><fmt:message key="div.minutes"/></span>
  </div>
</div>

<div>
  <label for="allowableLate" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.alat"/></label>
  <div class="flex items-center gap-2">
    <input id="allowableLate" name="allowableLate" type="number" value="4.0" step="0.1"
      title="How late a vehicle can arrive compared to the prediction and still be acceptable."
      class="form-control w-24"/>
    <span class="text-xs text-neutral-500"><fmt:message key="div.minutes"/></span>
  </div>
</div>
