<t:reportsParamsForm action="predAccuracyIntervalsChart.jsp">
  <jsp:attribute name="title"><fmt:message key="div.SpecifyParameters"/></jsp:attribute>
  <jsp:attribute name="heading"><fmt:message key="div.sppaic"/></jsp:attribute>
  <jsp:body>
    <jsp:include page="params/routeAllOrSingle.jsp"/>
    <jsp:include page="params/fromDateNumDaysTime.jsp"/>
    <jsp:include page="params/predictionSource.jsp"/>

    <%-- This page intentionally labels predictionType with div.psource
         ("Prediction Source") rather than div.ptype ("Prediction Type")
         for historical UX reasons; do not "correct" the key. --%>
    <div>
      <label for="predictionType" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.psource"/></label>
      <select id="predictionType" name="predictionType"
        title="Specifies whether or not to show prediction accuracy for predictions that were affected by a layover."
        class="form-control w-full max-w-md">
        <option value=""><fmt:message key="div.pall"/></option>
        <option value="AffectedByWaitStop"><fmt:message key="div.paff"/></option>
        <option value="NotAffectedByWaitStop"><fmt:message key="div.pnaff"/></option>
      </select>
    </div>

    <div>
      <label for="intervalsType" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.intt"/></label>
      <select id="intervalsType" name="intervalsType"
        title="Specifies the type of graph to be displayed."
        class="form-control w-full max-w-md">
        <option value="PERCENTAGE"><fmt:message key="div.pon"/></option>
        <option value="STD_DEV"><fmt:message key="div.sdon"/></option>
        <option value="BOTH"><fmt:message key="div.ponsd"/></option>
      </select>
    </div>

    <div>
      <label for="intervalPercentage1" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.iper"/>1:</label>
      <div class="flex items-center gap-2">
        <input id="intervalPercentage1" name="intervalPercentage1" type="number" value="70" min="0" max="100"
          title="For when using a 'Percentage' interval type. Specifies the percent of predictions that should lie within the minimum and maximum intervals."
          class="form-control w-24"/>
        <span class="text-xs text-neutral-500">%</span>
      </div>
    </div>

    <div>
      <label for="intervalPercentage2" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.iper"/>2:</label>
      <div class="flex items-center gap-2">
        <input id="intervalPercentage2" name="intervalPercentage2" type="number" min="0" max="100"
          title="Optional second percent of predictions that should lie within the minimum and maximum intervals."
          class="form-control w-24"/>
        <span class="text-xs text-neutral-500">%</span>
      </div>
    </div>

    <jsp:include page="params/submitReport.jsp"/>
  </jsp:body>
</t:reportsParamsForm>
