<%@ page import="org.transitclock.reports.ScheduleAdherenceController" %>
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
int scheduleEarlySec = ScheduleAdherenceController.getScheduleEarlySeconds();
int scheduleLateSec  = ScheduleAdherenceController.getScheduleLateSeconds();
pageContext.setAttribute("earlyMsec",   scheduleEarlySec * -1000);
pageContext.setAttribute("lateMsec",    scheduleLateSec  * 1000);
pageContext.setAttribute("scheduleEarlyMin", scheduleEarlySec / -60);
pageContext.setAttribute("scheduleLateMin",  scheduleLateSec  / 60);
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.acbiveblock" /></jsp:attribute>
  <jsp:body>
<div data-controller="active-blocks"
     data-action="accordion:opened->active-blocks#routeOpened"
     data-active-blocks-early-msec-value="${earlyMsec}"
     data-active-blocks-late-msec-value="${lateMsec}">

  <%-- Sticky summary header. Bleeds full-width within main by undoing the
       layout's p-8 padding via -mx-8/-mt-8, then re-applies px-8 itself. --%>
  <div data-active-blocks-target="summary"
       class="sticky top-0 z-10 -mx-8 -mt-8 mb-6 px-8 py-4 bg-white/95 backdrop-blur border-b border-gray-200">
    <div class="flex flex-wrap items-center gap-x-6 gap-y-2">
      <h1 class="mr-auto text-lg font-semibold text-gray-900"><fmt:message key="div.acbiveblock" /></h1>
      <dl class="flex flex-wrap items-center gap-x-5 gap-y-1 text-sm">
        <div class="flex items-baseline gap-1.5" title="Total number of blocks">
          <dt class="text-gray-500"><fmt:message key="div.blocks" /></dt>
          <dd data-field="total-blocks" class="font-mono text-gray-900">—</dd>
        </div>
        <div class="flex items-baseline gap-1.5" title="Percentage of blocks that have an assigned and predictable vehicle">
          <dt class="text-gray-500"><fmt:message key="div.assigned" /></dt>
          <dd data-field="percent-assigned" class="font-mono text-gray-900">—</dd>
        </div>
        <div class="flex items-baseline gap-1.5" title="Percentage of blocks where vehicle is more than ${scheduleLateMin} minutes late">
          <dt class="text-gray-500"><fmt:message key="div.clate" />:</dt>
          <dd data-field="percent-late" class="font-mono text-gray-900">—</dd>
        </div>
        <div class="flex items-baseline gap-1.5" title="Percentage of blocks where vehicle is on time">
          <dt class="text-gray-500"><fmt:message key="div.contime" />:</dt>
          <dd data-field="percent-on-time" class="font-mono text-gray-900">—</dd>
        </div>
        <div class="flex items-baseline gap-1.5" title="Percentage of blocks where vehicle is more than ${scheduleEarlyMin} minute(s) early">
          <dt class="text-gray-500"><fmt:message key="div.cearly" />:</dt>
          <dd data-field="percent-early" class="font-mono text-gray-900">—</dd>
        </div>
        <div class="flex items-baseline gap-1.5" title="Time that summary information was last updated">
          <dt class="text-gray-500"><fmt:message key="div.AsOf" /></dt>
          <dd data-field="as-of" class="font-mono text-gray-900">—</dd>
        </div>
      </dl>
      <button type="button"
              data-action="click->active-blocks#loadAll"
              data-active-blocks-target="loadAll"
              class="px-3 py-1.5 text-xs font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed">
        <fmt:message key="div.LoadAllData" />
      </button>
    </div>
  </div>

  <div data-controller="accordion"
       data-accordion-allow-multiple-value="true"
       data-active-blocks-target="accordion"
       class="bg-white border border-gray-200 rounded-lg divide-y divide-gray-200 overflow-hidden">
  </div>

  <template data-active-blocks-target="routeTemplate">
    <div data-accordion-target="item" data-state="closed" class="group">
      <h3 class="m-0">
        <button type="button"
                data-accordion-target="trigger"
                data-action="click->accordion#toggle"
                aria-expanded="false"
                data-state="closed"
                class="w-full flex items-center justify-between gap-4 px-4 py-3 text-left hover:bg-gray-50">
          <span class="flex items-center gap-3 min-w-0">
            <span data-field="route-name" class="text-sm font-semibold text-gray-900 truncate"></span>
          </span>
          <span class="flex items-center gap-3">
            <span data-vehicle-summary class="hidden items-center gap-2 text-xs">
              <span class="flex items-baseline gap-1">
                <span class="text-gray-500"><fmt:message key="div.cearly" /></span>
                <span data-field="route-early" class="px-1.5 py-0.5 rounded font-mono text-gray-900">0</span>
              </span>
              <span class="flex items-baseline gap-1">
                <span class="text-gray-500"><fmt:message key="div.contime" /></span>
                <span data-field="route-on-time" class="px-1.5 py-0.5 rounded font-mono text-gray-900">0</span>
              </span>
              <span class="flex items-baseline gap-1">
                <span class="text-gray-500"><fmt:message key="div.clate" /></span>
                <span data-field="route-late" class="px-1.5 py-0.5 rounded font-mono text-gray-900">0</span>
              </span>
              <span class="flex items-baseline gap-1">
                <span class="text-gray-500"><fmt:message key="div.assigned" /></span>
                <span data-field="route-vehicles" class="px-1.5 py-0.5 rounded font-mono text-gray-900">0</span>
              </span>
            </span>
            <span class="flex items-baseline gap-1 text-xs">
              <span class="text-gray-500"><fmt:message key="div.dblock" /></span>
              <span data-field="route-blocks" class="px-1.5 py-0.5 rounded font-mono text-gray-900">0</span>
            </span>
            <svg xmlns="http://www.w3.org/2000/svg" class="size-4 shrink-0 text-gray-400 transition-transform duration-200 group-data-[state=open]:rotate-180" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.168l3.71-3.938a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
            </svg>
          </span>
        </button>
      </h3>
      <div data-accordion-target="content"
           data-state="closed"
           hidden
           role="region"
           class="grid transition-[grid-template-rows] duration-300 ease-in-out data-[state=open]:grid-rows-[1fr] data-[state=closed]:grid-rows-[0fr]">
        <div class="overflow-hidden min-h-0">
          <div data-state="closed" class="px-4 py-3 space-y-2 bg-gray-50/50 transition-opacity duration-200 opacity-0 data-[state=open]:opacity-100">
            <div data-block-list class="space-y-2"></div>
          </div>
        </div>
      </div>
    </div>
  </template>

  <template data-active-blocks-target="blockTemplate">
    <div class="bg-white border border-gray-200 rounded-md px-3 py-2 grid grid-cols-2 md:grid-cols-4 gap-x-4 gap-y-1.5 text-xs">
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.dblock" />:</dt>
        <dd data-field="block-id" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.Start" /></dt>
        <dd data-field="block-start" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.End" /></dt>
        <dd data-field="block-end" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.Service" /></dt>
        <dd data-field="block-service" class="font-mono text-gray-900 truncate"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.dtrip" />:</dt>
        <dd data-field="trip-id" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.Start" /></dt>
        <dd data-field="trip-start" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5">
        <dt class="text-gray-500"><fmt:message key="div.End" /></dt>
        <dd data-field="trip-end" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5 col-span-2 md:col-span-1">
        <dt class="text-gray-500"><fmt:message key="div.Headsign" />:</dt>
        <dd data-field="trip-headsign" class="text-gray-900 truncate"></dd>
      </div>
      <div class="flex items-baseline gap-1.5 col-span-2">
        <dt class="text-gray-500"><fmt:message key="div.Vehicle" />:</dt>
        <dd data-field="block-vehicles" class="font-mono text-gray-900"></dd>
      </div>
      <div class="flex items-baseline gap-1.5 col-span-2">
        <dt class="text-gray-500"><fmt:message key="div.Adh" /></dt>
        <dd data-field="block-sch-adh" class="px-1.5 py-0.5 rounded font-mono"></dd>
      </div>
    </div>
  </template>
</div>
  </jsp:body>
</t:layout>
