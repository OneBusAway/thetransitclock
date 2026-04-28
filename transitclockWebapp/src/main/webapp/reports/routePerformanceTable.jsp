<%@ page import="org.transitclock.utils.web.WebUtils" %>
<t:reportsLayout>
  <jsp:attribute name="title">Route Performance Report</jsp:attribute>
  <jsp:body>
    <header class="border-b border-gray-200 px-6 py-4">
      <h1 class="text-xl font-semibold text-gray-900">Route Performance Table</h1>
    </header>
    <div class="px-6 py-6">
      <div id="menu">
        <form class="max-w-2xl space-y-5">
          <input type="hidden" name="a" value="${param.a}">
          <jsp:include page="params/fromDateNumDaysTime.jsp"/>
          <jsp:include page="params/predictionSource.jsp"/>

          <div>
            <label for="predictionType" class="block text-sm font-medium text-neutral-700 mb-1"><fmt:message key="div.ptype"/></label>
            <select id="predictionType" name="predictionType"
              title="Specifies whether or not to show prediction accuracy for predictions that were affected by a layover."
              class="form-control w-full max-w-md">
              <option value=""><fmt:message key="div.pall"/></option>
              <option value="AffectedByWaitStop"><fmt:message key="div.paff"/></option>
              <option value="NotAffectedByWaitStop"><fmt:message key="div.pnaff"/></option>
            </select>
          </div>

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
        </form>

        <div class="pt-4">
          <button type="button" id="submit"
            class="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-xs hover:bg-indigo-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600">Update report</button>
        </div>
      </div>

      <div class="mt-8">
        <img src="images/page-loader.gif" id="loading" class="mx-auto"/>
        <div id="tableDiv" class="mt-4"></div>
      </div>
    </div>

    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript" src="javascript/routePerformanceTable.js"></script>
  </jsp:body>
</t:reportsLayout>
