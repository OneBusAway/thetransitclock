<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="head"  fragment="true" required="false" %>
<t:splitLayout title="${title}">
  <jsp:attribute name="head"><jsp:invoke fragment="head"/></jsp:attribute>
  <jsp:attribute name="sidebar"><t:reportsSidebar/></jsp:attribute>
  <jsp:body><jsp:doBody/></jsp:body>
</t:splitLayout>
