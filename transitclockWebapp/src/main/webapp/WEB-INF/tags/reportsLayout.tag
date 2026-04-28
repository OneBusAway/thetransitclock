<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t"   tagdir="/WEB-INF/tags" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="head"  fragment="true" required="false" %>
<%-- bare="true" so the inner sidebar+content flex container can fill
     the main pane itself; the default p-8 wrapper would double-pad. --%>
<t:layout title="${title}" bare="true">
  <jsp:attribute name="head">
    <jsp:invoke fragment="head"/>
  </jsp:attribute>
  <jsp:body>
    <div class="flex h-full">
      <t:reportsSidebar/>
      <div class="flex-1 overflow-y-auto">
        <jsp:doBody/>
      </div>
    </div>
  </jsp:body>
</t:layout>
