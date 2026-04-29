<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
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
pageContext.setAttribute("currentYear",      java.time.Year.now().getValue());
%>
<t:layout>
  <jsp:attribute name="title"><fmt:message key="div.acbiveblock" /></jsp:attribute>
  <jsp:body>
<div data-controller="active-blocks"
     data-action="accordion:opened->active-blocks#routeOpened"
     data-active-blocks-early-msec-value="${earlyMsec}"
     data-active-blocks-late-msec-value="${lateMsec}">

  <div class="flex items-end justify-between gap-4 flex-wrap mb-4">
    <div>
      <div class="flex items-center gap-1.5 text-xs text-gray-500 font-medium mb-1.5">
        <fmt:message key="div.status"/>
        <svg class="size-3 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 6 6 6-6 6"/></svg>
        <span class="text-gray-700"><fmt:message key="div.acbiveblock"/></span>
      </div>
      <h1 class="text-2xl font-bold tracking-tight text-gray-900 m-0"><fmt:message key="div.acbiveblock"/></h1>
      <p class="mt-1 text-sm text-gray-500 m-0">Real-time status of vehicle assignments across the fleet.</p>
    </div>
    <div class="flex items-center gap-3">
      <span class="text-xs text-gray-500 inline-flex items-center gap-1.5">
        <svg class="size-3 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
        <fmt:message key="div.AsOf"/>
        <span data-active-blocks-target="asOf" class="font-semibold text-gray-700 tabular-nums">—</span>
      </span>
      <button type="button"
              data-action="click->active-blocks#loadAll"
              data-active-blocks-target="loadAll"
              class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md border-0 bg-brand-accent text-white text-sm font-semibold shadow-sm hover:bg-brand disabled:opacity-50 disabled:cursor-not-allowed">
        <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 4v12m0 0-4-4m4 4 4-4M4 20h16"/></svg>
        <fmt:message key="div.LoadAllData"/>
      </button>
    </div>
  </div>

  <%-- Bleeds out of the layout's p-8 padding via -mx-8 so the bar spans
       the full main width and stays flush at the scroll-container edge. --%>
  <div class="sticky top-0 z-10 -mx-8 mb-5 px-8 pt-3 pb-3 bg-canvas/90 backdrop-blur">
    <t:activeBlocksSummary/>
  </div>

  <div data-controller="accordion"
       data-accordion-allow-multiple-value="true"
       data-active-blocks-target="accordion"
       class="space-y-2">
    <t:loading/>
  </div>

  <%-- Block list inside the body hydrates lazily on first accordion open
       via the `accordion:opened` event wired up at the controller root. --%>
  <template data-active-blocks-target="routeTemplate">
    <div data-accordion-target="item" data-state="closed" class="group bg-white border border-gray-200 rounded-lg overflow-hidden shadow-[0_1px_2px_rgba(0,0,0,0.04)]">
      <h3 class="m-0">
        <button type="button"
                data-accordion-target="trigger"
                data-action="click->accordion#toggle"
                aria-expanded="false"
                data-state="closed"
                class="w-full grid grid-cols-[4px_auto_1fr_auto_auto] items-center gap-3.5 pl-0 pr-3 py-2.5 text-left bg-white hover:bg-[#fafbf8]">
          <span data-field="route-stripe" class="block w-1 h-8 rounded-r-sm bg-gray-200"></span>
          <span data-field="route-tag" class="inline-flex items-center justify-center min-w-[44px] px-2 py-0.5 rounded bg-brand-accent text-white text-xs font-bold tracking-tight ml-2">—</span>
          <span class="flex items-baseline gap-2.5 min-w-0">
            <span data-field="route-name" class="text-[15px] font-semibold text-gray-900 truncate"></span>
            <span class="text-xs text-gray-500 whitespace-nowrap">
              <span data-field="route-blocks" class="tabular-nums">0</span>
              <span data-field="route-blocks-noun"><fmt:message key="div.dblock"/></span>
            </span>
          </span>
          <span data-vehicle-summary class="hidden items-center gap-3.5 text-xs">
            <span data-field="route-early-pair" class="hidden items-center gap-1">
              <span class="text-gray-500"><fmt:message key="div.cearly"/></span>
              <span data-field="route-early" class="font-bold text-status-early tabular-nums">0</span>
            </span>
            <span data-field="route-on-time-pair" class="hidden items-center gap-1">
              <span class="text-gray-500"><fmt:message key="div.contime"/></span>
              <span data-field="route-on-time" class="font-bold text-status-on-time-ink tabular-nums">0</span>
            </span>
            <span data-field="route-late-pair" class="hidden items-center gap-1">
              <span class="text-gray-500"><fmt:message key="div.clate"/></span>
              <span data-field="route-late" class="font-bold text-status-late tabular-nums">0</span>
            </span>
          </span>
          <span class="flex items-center gap-3 pr-1">
            <span class="text-xs text-gray-500 font-medium hidden sm:inline-flex items-baseline gap-1">
              <fmt:message key="div.assigned"/>
              <span class="text-gray-900 font-bold tabular-nums"><span data-field="route-vehicles">0</span>/<span data-field="route-blocks-denom">0</span></span>
            </span>
            <svg class="size-4 shrink-0 text-gray-400 transition-transform duration-200 group-data-[state=open]:rotate-90" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m9 6 6 6-6 6"/>
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
          <div data-state="closed" class="bg-[#fafbf8] border-t border-gray-200 transition-opacity duration-200 opacity-0 data-[state=open]:opacity-100">
            <div data-block-list></div>
          </div>
        </div>
      </div>
    </div>
  </template>

  <template data-active-blocks-target="blockTemplate">
    <div class="grid grid-cols-2 md:grid-cols-[0.9fr_1.4fr_1.6fr_1fr_1.6fr] gap-x-4 gap-y-2 px-4 py-3 border-t border-gray-100 bg-white text-[13px]">
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.dblock"/></div>
        <div data-field="block-id" class="font-semibold font-mono text-gray-900"></div>
        <div class="text-xs text-gray-500 mt-0.5"><fmt:message key="div.dtrip"/> <span data-field="trip-id" class="font-mono"></span></div>
      </div>
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.Vehicle"/></div>
        <div class="flex items-center gap-1.5 mt-0.5">
          <svg class="size-3.5 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="4" width="16" height="14" rx="2"/><path d="M4 11h16M7 18v2M17 18v2M9 15h.01M15 15h.01"/></svg>
          <span data-field="block-vehicles" class="font-semibold font-mono text-gray-900"></span>
        </div>
        <div class="text-xs text-gray-500 mt-0.5"><fmt:message key="div.Service"/> <span data-field="block-service" class="font-mono"></span></div>
      </div>
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.Schedule"/></div>
        <div class="flex items-center gap-1.5 mt-0.5 font-mono text-[12.5px] text-gray-900">
          <span data-field="block-start"></span>
          <span class="text-gray-400">&rarr;</span>
          <span data-field="block-end"></span>
        </div>
        <div class="text-xs text-gray-500 mt-0.5">
          <fmt:message key="div.dtrip"/> <span data-field="trip-start" class="font-mono"></span>
          <span class="text-gray-400">&rarr;</span>
          <span data-field="trip-end" class="font-mono"></span>
        </div>
      </div>
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.Adh"/></div>
        <div class="mt-1"><span data-field="block-sch-adh" class="inline-flex items-center gap-1.5 px-1.5 py-0.5 rounded font-mono text-xs font-semibold"></span></div>
      </div>
      <div>
        <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.Headsign"/></div>
        <div data-field="trip-headsign" class="text-gray-900 font-medium mt-0.5 truncate"></div>
      </div>
    </div>
  </template>

  <footer class="mt-8 pt-4 border-t border-gray-200 flex justify-between text-xs text-gray-500">
    <span>OneBusAway &middot; The Transit Clock</span>
    <span>OTSF &copy; ${currentYear}</span>
  </footer>
</div>
  </jsp:body>
</t:layout>
