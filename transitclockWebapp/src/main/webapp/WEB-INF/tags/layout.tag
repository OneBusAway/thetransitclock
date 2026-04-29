<%@ tag pageEncoding="UTF-8" import="org.transitclock.db.webstructs.WebAgency" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="head"  fragment="true" required="false" %>
<%-- bare="true" skips the default p-8 padding wrapper around the body — for
     map and other full-bleed pages that need to fill the main pane. --%>
<%@ attribute name="bare"  required="false" type="java.lang.Boolean" %>
<%-- Fetch agencies in the layout's own pageContext so the sidebar
     renders for every page that uses <t:layout>. --%>
<% jspContext.setAttribute("webAgencies", WebAgency.getCachedOrderedListOfWebAgencies()); %>
<fmt:setLocale value="${pageContext.request.locale}" />

<%-- currentAgency may be empty (e.g. welcome page) — the brand bar
     renders product-name-only in that case. --%>
<c:forEach var="agency" items="${webAgencies}">
  <c:if test="${agency.active and agency.agencyId == param.a}">
    <c:set var="currentAgency" value="${agency}"/>
  </c:if>
</c:forEach>

<%-- Sidebar selection state. The on* flags identify the current page from
     the request path/query; the agency-scoped *Active flags are computed
     inside the per-agency forEach below so only the active agency's group
     highlights and expands. --%>
<c:set var="path"           value="${pageContext.request.servletPath}"/>
<c:set var="showUnassigned" value="${param.showUnassignedVehicles == 'true'}"/>
<c:set var="onMapFor"       value="${path == '/maps/map.jsp' and not showUnassigned}"/>
<c:set var="onMapIncl"      value="${path == '/maps/map.jsp' and showUnassigned}"/>
<c:set var="onSchAdhMap"    value="${path == '/maps/schAdhMap.jsp'}"/>
<c:set var="onApi"          value="${fn:startsWith(path, '/reports/apiCalls/')}"/>
<%-- Any /reports/* page (except the API calls section) keeps the
     Reports nav highlighted so subpages don't appear unrooted. --%>
<c:set var="onReports"      value="${fn:startsWith(path, '/reports/') and not onApi}"/>
<c:set var="onDashboard"     value="${path == '/dashboard/index.jsp'}"/>
<c:set var="onActiveBlocks" value="${path == '/status/activeBlocks.jsp'}"/>
<c:set var="onServerStatus" value="${path == '/status/serverStatus.jsp'}"/>
<c:set var="onDbDiskSpace"  value="${path == '/status/dbDiskSpace.jsp'}"/>
<c:set var="onSynoptic"     value="${path == '/synoptic/index.jsp'}"/>
<%-- The left-edge pill on active items is an absolutely-positioned span
     inside each link, which is why every link uses `relative`. --%>
<c:set var="navBase"     value="relative flex items-center gap-2.5 px-2.5 py-1.5 rounded-md text-sm transition-colors"/>
<c:set var="navInactive" value="text-gray-700 hover:bg-gray-100 hover:text-gray-900"/>
<c:set var="navActive"   value="bg-brand-tint text-brand-accent font-semibold"/>
<c:set var="iconActive"  value="text-brand-accent"/>
<c:set var="iconIdle"    value="text-gray-500"/>

<!DOCTYPE html>
<html class="h-full">
<head>
<title>${title}</title>
<%@ include file="/template/includes.jsp" %>
<jsp:invoke fragment="head"/>
</head>
<body class="h-full overflow-hidden flex flex-col bg-canvas">

<header class="shrink-0 flex items-center justify-between h-14 px-6 bg-brand text-white border-b border-black/10 z-30">
  <a href="${pageContext.request.contextPath}/" class="flex items-center gap-3.5 text-white no-underline">
    <span class="size-9 rounded-md bg-white flex items-center justify-center shadow-[0_0_0_1px_rgba(255,255,255,0.2)]">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#78aa36" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <rect x="4" y="4" width="16" height="14" rx="2"/>
        <path d="M4 11h16M7 18v2M17 18v2M9 15h.01M15 15h.01"/>
      </svg>
    </span>
    <span class="flex items-center gap-2.5">
      <c:choose>
        <c:when test="${not empty currentAgency}">
          <span class="text-lg font-extrabold tracking-tight"><c:out value="${fn:toUpperCase(currentAgency.agencyName)}"/></span>
          <span class="opacity-55 text-sm">·</span>
          <span class="text-sm font-medium opacity-95">The Transit Clock</span>
        </c:when>
        <c:otherwise>
          <span class="text-lg font-extrabold tracking-tight">The Transit Clock</span>
        </c:otherwise>
      </c:choose>
    </span>
  </a>
  <div class="flex items-center gap-3 text-sm">
    <div class="flex items-center gap-2 px-2.5 py-1.5 rounded-md bg-white/15 font-semibold tabular-nums">
      <span class="size-1.5 rounded-full bg-emerald-300 ring-2 ring-emerald-300/30" aria-hidden="true"></span>
      <span>Live · <span data-tc-clock>--:--:-- --</span></span>
    </div>
  </div>
</header>

<div class="flex flex-1 min-h-0">
  <aside class="hidden md:flex md:flex-col w-60 shrink-0 border-r border-gray-200 bg-white">
    <nav class="flex-1 overflow-y-auto px-3 py-5 space-y-5">
      <c:forEach var="agency" items="${webAgencies}">
        <c:if test="${agency.active}">
          <c:set var="qs" value="?a=${agency.agencyId}"/>
          <c:set var="ctx" value="${pageContext.request.contextPath}"/>
          <c:set var="agencyMatch"        value="${param.a == agency.agencyId}"/>
          <c:set var="mapForActive"       value="${onMapFor and agencyMatch}"/>
          <c:set var="mapInclActive"      value="${onMapIncl and agencyMatch}"/>
          <c:set var="schAdhMapActive"    value="${onSchAdhMap and agencyMatch}"/>
          <c:set var="mapsExpanded"       value="${mapForActive or mapInclActive or schAdhMapActive}"/>
          <c:set var="reportsActive"      value="${onReports and agencyMatch}"/>
          <c:set var="apiActive"          value="${onApi and agencyMatch}"/>
          <c:set var="dashboardActive"    value="${onDashboard and agencyMatch}"/>
          <c:set var="activeBlocksActive" value="${onActiveBlocks and agencyMatch}"/>
          <c:set var="serverStatusActive" value="${onServerStatus and agencyMatch}"/>
          <c:set var="dbDiskSpaceActive"  value="${onDbDiskSpace and agencyMatch}"/>
          <c:set var="synopticActive"     value="${onSynoptic and agencyMatch}"/>

          <div>
            <h3 class="px-2.5 mb-2 text-[11px] font-bold tracking-[0.08em] uppercase text-gray-500"><c:out value="${agency.agencyName}"/></h3>
            <ul class="space-y-0.5">
              <li>
                <a class="${navBase} ${dashboardActive ? navActive : navInactive}" <c:if test="${dashboardActive}">aria-current="page"</c:if> href="${ctx}/dashboard/index.jsp${qs}">
                  <c:if test="${dashboardActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${dashboardActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></svg>
                  <fmt:message key="div.dashboard"/>
                </a>
              </li>
              <li data-controller="disclosure">
                <button type="button" aria-expanded="${mapsExpanded}"
                        data-action="click->disclosure#toggle"
                        class="w-full ${navBase} ${navInactive} justify-between">
                  <span class="flex items-center gap-2.5">
                    <svg class="size-4 ${iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14"/></svg>
                    <fmt:message key="div.maps"/>
                  </span>
                  <svg data-disclosure-target="chevron" class="size-3 text-gray-400 transition-transform ${mapsExpanded ? 'rotate-90' : ''}" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
                  </svg>
                </button>
                <ul data-disclosure-target="panel" class="${mapsExpanded ? '' : 'hidden'} mt-1 ml-7 space-y-0.5">
                  <li>
                    <a class="${navBase} ${mapForActive ? navActive : navInactive}" <c:if test="${mapForActive}">aria-current="page"</c:if> href="${ctx}/maps/map.jsp${qs}&verbose=true">
                      <c:if test="${mapForActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                      <fmt:message key="div.mapfor"/>
                    </a>
                  </li>
                  <li>
                    <a class="${navBase} ${mapInclActive ? navActive : navInactive}" <c:if test="${mapInclActive}">aria-current="page"</c:if> href="${ctx}/maps/map.jsp${qs}&verbose=true&showUnassignedVehicles=true">
                      <c:if test="${mapInclActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                      <fmt:message key="div.mapincluding"/>
                    </a>
                  </li>
                  <li>
                    <a class="${navBase} ${schAdhMapActive ? navActive : navInactive}" <c:if test="${schAdhMapActive}">aria-current="page"</c:if> href="${ctx}/maps/schAdhMap.jsp${qs}">
                      <c:if test="${schAdhMapActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                      <fmt:message key="div.ScheduleAdherenceMap"/>
                    </a>
                  </li>
                </ul>
              </li>
              <li>
                <a class="${navBase} ${reportsActive ? navActive : navInactive}" <c:if test="${reportsActive}">aria-current="page"</c:if> href="${ctx}/reports/index.jsp${qs}">
                  <c:if test="${reportsActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${reportsActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 21h18M5 21V10m6 11V5m6 16v-8"/></svg>
                  <fmt:message key="div.reports"/>
                </a>
              </li>
              <li>
                <a class="${navBase} ${apiActive ? navActive : navInactive}" <c:if test="${apiActive}">aria-current="page"</c:if> href="${ctx}/reports/apiCalls/index.jsp${qs}">
                  <c:if test="${apiActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${apiActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m8 8-5 4 5 4M16 8l5 4-5 4M14 4l-4 16"/></svg>
                  <fmt:message key="div.api"/>
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 class="px-2.5 mb-2 text-[11px] font-bold tracking-[0.08em] uppercase text-gray-500"><fmt:message key="div.status"/></h3>
            <ul class="space-y-0.5">
              <li>
                <a class="${navBase} ${activeBlocksActive ? navActive : navInactive}" <c:if test="${activeBlocksActive}">aria-current="page"</c:if> href="${ctx}/status/activeBlocks.jsp${qs}">
                  <c:if test="${activeBlocksActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${activeBlocksActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="4" y="4" width="16" height="14" rx="2"/><path d="M4 11h16M7 18v2M17 18v2M9 15h.01M15 15h.01"/></svg>
                  <fmt:message key="div.acbiveblock"/>
                </a>
              </li>
              <li>
                <a class="${navBase} ${serverStatusActive ? navActive : navInactive}" <c:if test="${serverStatusActive}">aria-current="page"</c:if> href="${ctx}/status/serverStatus.jsp${qs}">
                  <c:if test="${serverStatusActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${serverStatusActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="7" rx="1"/><rect x="3" y="13" width="18" height="7" rx="1"/><path d="M7 7.5h.01M7 16.5h.01"/></svg>
                  <fmt:message key="div.ss"/>
                </a>
              </li>
              <li>
                <a class="${navBase} ${dbDiskSpaceActive ? navActive : navInactive}" <c:if test="${dbDiskSpaceActive}">aria-current="page"</c:if> href="${ctx}/status/dbDiskSpace.jsp${qs}">
                  <c:if test="${dbDiskSpaceActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${dbDiskSpaceActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><ellipse cx="12" cy="6" rx="8" ry="3"/><path d="M4 6v12c0 1.7 3.6 3 8 3s8-1.3 8-3V6M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3"/></svg>
                  <fmt:message key="div.ddsu"/>
                </a>
              </li>
            </ul>
          </div>

          <div>
            <h3 class="px-2.5 mb-2 text-[11px] font-bold tracking-[0.08em] uppercase text-gray-500"><fmt:message key="div.operations"/></h3>
            <ul class="space-y-0.5">
              <li>
                <a class="${navBase} ${synopticActive ? navActive : navInactive}" <c:if test="${synopticActive}">aria-current="page"</c:if> href="${ctx}/synoptic/index.jsp${qs}">
                  <c:if test="${synopticActive}"><span class="absolute left-0 top-1.5 bottom-1.5 w-[3px] rounded-r-sm bg-brand-accent"></span></c:if>
                  <svg class="size-4 ${synopticActive ? iconActive : iconIdle}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
                  <fmt:message key="div.synoptic"/>
                </a>
              </li>
            </ul>
          </div>
        </c:if>
      </c:forEach>
    </nav>
  </aside>
  <main class="flex-1 min-w-0 overflow-y-auto">
    <c:choose>
      <c:when test="${bare}"><jsp:doBody/></c:when>
      <c:otherwise><div class="p-8"><jsp:doBody/></div></c:otherwise>
    </c:choose>
  </main>
</div>

<%-- Inlined because it's tiny and only one page-global element needs it;
     not worth a separate Stimulus controller. --%>
<script>
(function () {
  var el = document.querySelector('[data-tc-clock]');
  if (!el) return;
  var pad = function (n) { return String(n).padStart(2, '0'); };
  var tick = function () {
    var d = new Date();
    var h = d.getHours();
    var ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    el.textContent = pad(h) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds()) + ' ' + ampm;
  };
  tick();
  setInterval(tick, 1000);
})();
</script>

</body>
</html>
