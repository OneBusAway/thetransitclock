<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ attribute name="title"   required="false" %>
<%@ attribute name="head"    fragment="true" required="false" %>
<%@ attribute name="sidebar" fragment="true" required="true" %>
<%-- Two-pane layout: a middle sidebar (passed in via the `sidebar` fragment)
     and the main content (body). Used by index pages that present a list of
     reports or API calls alongside an empty-state / detail pane.
     bare="true" so the sidebar+content flex container can fill the main
     pane itself; the default p-8 wrapper would double-pad. --%>
<t:layout title="${title}" bare="true">
  <jsp:attribute name="head">
    <jsp:invoke fragment="head"/>
  </jsp:attribute>
  <jsp:body>
    <div class="flex h-full">
      <jsp:invoke fragment="sidebar"/>
      <div class="flex-1 overflow-y-auto">
        <jsp:doBody/>
      </div>
    </div>
  </jsp:body>
</t:layout>
