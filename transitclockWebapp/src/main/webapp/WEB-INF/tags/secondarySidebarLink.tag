<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="path"       required="true"  %>
<%@ attribute name="title"      required="false" %>
<%@ attribute name="omitAgency" required="false" type="java.lang.Boolean" %>
<%-- Active-state link for use inside <t:secondarySidebar>. `path` is matched
     against the current servletPath to highlight the row, and is also the
     href. `omitAgency="true"` drops the ?a=... query string (e.g. for
     /reports/apiCalls/agenciesApiParams.jsp, which lists all agencies and
     therefore takes no agency param). The body provides the link label. --%>
<c:set var="curPath" value="${pageContext.request.servletPath}"/>
<c:set var="ctx"     value="${pageContext.request.contextPath}"/>
<li><a class="block px-2 py-1.5 text-sm rounded ${curPath == path ? 'bg-brand-tint text-brand-accent font-medium' : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'}" href="${ctx}${path}<c:if test="${not omitAgency and not empty param.a}">?a=${param.a}</c:if>"<c:if test="${not empty title}"> title="${title}"</c:if>><jsp:doBody/></a></li>
