<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="head"  fragment="true" required="false" %>
<%-- API call sub-pages use this so the API Calls sidebar stays visible
     when drilling into a specific call, mirroring how reports/* pages
     keep the Reports sidebar visible. The p-8 wrapper restores the
     padding that t:layout gives by default and that t:splitLayout
     (which is bare) does not. --%>
<t:splitLayout title="${title}">
  <jsp:attribute name="head"><jsp:invoke fragment="head"/></jsp:attribute>
  <jsp:attribute name="sidebar"><t:apiCallsSidebar/></jsp:attribute>
  <jsp:body><div class="p-8"><jsp:doBody/></div></jsp:body>
</t:splitLayout>
