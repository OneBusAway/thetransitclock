<%@ page import="org.transitclock.db.webstructs.WebAgency" %>
<fmt:setLocale value="${pageContext.request.locale}" />
<% pageContext.setAttribute("webAgencies", WebAgency.getCachedOrderedListOfWebAgencies()); %>
<t:layout title="Agencies">
  <style>
  #agencyList {
    margin-left: auto;
    margin-right: auto;
  }
  table { border-spacing: 0px; }
  td {
    padding: 4px 16px;
    text-align: left;
  }
  tr:nth-child(odd)  { background: #F6F6F6 }
  tr:nth-child(even) { background: #EBEBEB }
  #agencyName {
    width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  </style>

  <div id="title"><fmt:message key="div.agencies" /></div>
  <table id="agencyList">
    <c:forEach var="agency" items="${webAgencies}">
      <c:if test="${agency.active}">
        <c:url var="qs" value="?a=${agency.agencyId}"/>
        <tr>
          <td><div id="agencyName"><c:out value="${agency.agencyName}"/></div></td>
          <td><a href="${pageContext.request.contextPath}/maps/index.jsp${qs}"             title="Real-time maps"><fmt:message key="div.maps" /></a></td>
          <td><a href="${pageContext.request.contextPath}/reports/index.jsp${qs}"          title="Reports on historic information"><fmt:message key="div.reports" /></a></td>
          <td><a href="${pageContext.request.contextPath}/reports/apiCalls/index.jsp${qs}" title="API calls"><fmt:message key="div.api" /></a></td>
          <td><a href="${pageContext.request.contextPath}/status/index.jsp${qs}"           title="Pages showing current status of system"><fmt:message key="div.status" /></a></td>
          <td><a href="${pageContext.request.contextPath}/synoptic/index.jsp${qs}"         title="Real-time synoptic"><fmt:message key="div.synoptic" /></a></td>
          <td><a href="${pageContext.request.contextPath}/extensions/index.jsp${qs}"       title="Page of links to extension to the system"><fmt:message key="div.extensions" /></a></td>
        </tr>
      </c:if>
    </c:forEach>
  </table>
</t:layout>
