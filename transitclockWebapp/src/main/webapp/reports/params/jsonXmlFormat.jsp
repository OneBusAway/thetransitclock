<%-- Output format radio buttons (JSON / XML). --%>
<fieldset>
  <legend class="block text-sm font-medium text-neutral-700 mb-1">Format</legend>
  <div class="flex items-center gap-6">
    <label class="inline-flex items-center gap-2 text-sm text-neutral-700">
      <input type="radio" name="format" value="json" checked
        class="h-4 w-4 border-neutral-300 text-neutral-700 focus:ring-neutral-600"/>
      <span>JSON</span>
    </label>
    <label class="inline-flex items-center gap-2 text-sm text-neutral-700">
      <input type="radio" name="format" value="xml"
        class="h-4 w-4 border-neutral-300 text-neutral-700 focus:ring-neutral-600"/>
      <span>XML</span>
    </label>
  </div>
</fieldset>
