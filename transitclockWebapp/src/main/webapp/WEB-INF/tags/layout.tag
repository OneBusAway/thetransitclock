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
          <c:url var="qs" value="?a=${agency.agencyId}"/>
          <div>
            <h3 class="px-2 mb-1 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <c:out value="${agency.agencyName}"/>
            </h3>
            <ul class="space-y-0.5">
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/maps/index.jsp${qs}"><fmt:message key="div.maps"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/reports/index.jsp${qs}"><fmt:message key="div.reports"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/reports/apiCalls/index.jsp${qs}"><fmt:message key="div.api"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/status/index.jsp${qs}"><fmt:message key="div.status"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/synoptic/index.jsp${qs}"><fmt:message key="div.synoptic"/></a></li>
              <li><a class="block px-2 py-1.5 text-sm text-gray-700 rounded hover:bg-gray-100 hover:text-gray-900" href="${pageContext.request.contextPath}/extensions/index.jsp${qs}"><fmt:message key="div.extensions"/></a></li>
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
