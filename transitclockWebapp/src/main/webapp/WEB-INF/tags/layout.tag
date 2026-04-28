<%@ tag pageEncoding="UTF-8" import="org.transitclock.db.webstructs.WebAgency" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="head"  fragment="true" required="false" %>
<%-- Fetch agencies in the layout's own pageContext so the sidebar
     renders for every page that uses <t:layout>. --%>
<% jspContext.setAttribute("webAgencies", WebAgency.getCachedOrderedListOfWebAgencies()); %>
<fmt:setLocale value="${pageContext.request.locale}" />
<!DOCTYPE html>
<html class="h-full bg-white">
<head>
<title>${title}</title>
<%@ include file="/template/includes.jsp" %>
<jsp:invoke fragment="head"/>
</head>
<body class="h-full">
<div class="flex h-full">
  <aside class="hidden md:flex md:flex-col w-64 shrink-0 border-r border-gray-200 bg-white">
    <div class="flex items-center h-16 px-6 border-b border-gray-200">
      <a href="${pageContext.request.contextPath}/" class="text-base font-semibold text-gray-900">The Transit Clock</a>
    </div>
    <nav class="flex-1 overflow-y-auto px-3 py-4 space-y-6">
      <c:forEach var="agency" items="${webAgencies}">
        <c:if test="${agency.active}">
          <c:set var="qs" value="?a=${agency.agencyId}"/>
          <c:set var="ctx" value="${pageContext.request.contextPath}"/>
          <div>
            <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <c:out value="${agency.agencyName}"/>
            </h3>
            <ul class="space-y-0.5">
              <li data-controller="disclosure">
                <button type="button" aria-expanded="false"
                        data-action="click->disclosure#toggle"
                        class="w-full flex items-center justify-between px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900">
                  <span><fmt:message key="div.maps"/></span>
                  <svg data-disclosure-target="chevron" class="w-3 h-3 text-gray-400 transition-transform" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
                  </svg>
                </button>
                <ul data-disclosure-target="panel" class="hidden mt-1 ml-3 space-y-0.5">
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/maps/map.jsp${qs}&verbose=true"><fmt:message key="div.mapfor"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/maps/map.jsp${qs}&verbose=true&showUnassignedVehicles=true"><fmt:message key="div.mapincluding"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/maps/schAdhMap.jsp${qs}"><fmt:message key="div.ScheduleAdherenceMap"/></a></li>
                </ul>
              </li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/reports/index.jsp${qs}"><fmt:message key="div.reports"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/reports/apiCalls/index.jsp${qs}"><fmt:message key="div.api"/></a></li>
              <li data-controller="disclosure">
                <button type="button" aria-expanded="false"
                        data-action="click->disclosure#toggle"
                        class="w-full flex items-center justify-between px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900">
                  <span><fmt:message key="div.status"/></span>
                  <svg data-disclosure-target="chevron" class="w-3 h-3 text-gray-400 transition-transform" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
                  </svg>
                </button>
                <ul data-disclosure-target="panel" class="hidden mt-1 ml-3 space-y-0.5">
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/status/activeBlocks.jsp${qs}"><fmt:message key="div.acbiveblock"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/maps/schAdhMap.jsp${qs}"><fmt:message key="div.ScheduleAdherenceMap"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/status/serverStatus.jsp${qs}"><fmt:message key="div.ss"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/status/dbDiskSpace.jsp${qs}"><fmt:message key="div.ddsu"/></a></li>
                </ul>
              </li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/synoptic/index.jsp${qs}"><fmt:message key="div.synoptic"/></a></li>
              <%-- holding URLs are hardcoded for VIA (agency=1, route=100). --%>
              <li data-controller="disclosure">
                <button type="button" aria-expanded="false"
                        data-action="click->disclosure#toggle"
                        class="w-full flex items-center justify-between px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900">
                  <span><fmt:message key="div.extensions"/></span>
                  <svg data-disclosure-target="chevron" class="w-3 h-3 text-gray-400 transition-transform" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                    <path fill-rule="evenodd" d="M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" clip-rule="evenodd"/>
                  </svg>
                </button>
                <ul data-disclosure-target="panel" class="hidden mt-1 ml-3 space-y-0.5">
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/holding/singlestopholding.html?agency=1&route=100&stop=20097&agencyname=VIA&stopname=TEXAS%20MEDICAL%20CENTER&threshold=10000"><fmt:message key="div.htns"/></a></li>
                  <li><a class="block px-2 py-1 text-sm text-gray-600 rounded hover:bg-gray-100 hover:text-gray-900" href="${ctx}/holding/singlestopholding.html?agency=1&route=100&stop=93296&agencyname=VIA&stopname=CHESTNUT%20AT%20ELLIS%20ALLEY&threshold=10000"><fmt:message key="div.htss"/></a></li>
                </ul>
              </li>
            </ul>
          </div>
        </c:if>
      </c:forEach>
    </nav>
  </aside>
  <main class="flex-1 overflow-y-auto">
    <div class="p-8">
      <jsp:doBody/>
    </div>
  </main>
</div>
</body>
</html>
