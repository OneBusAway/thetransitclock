<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="title"   required="false" %>
<%@ attribute name="heading" required="true"  %>
<%@ attribute name="action"  required="true"  %>
<%@ attribute name="select2" required="false" type="java.lang.Boolean" %>
<%@ attribute name="head"    fragment="true" required="false" %>
<c:set var="useSelect2"     value="${select2 ne false}"/>
<c:set var="effectiveTitle" value="${empty title ? heading : title}"/>
<t:reportsLayout title="${effectiveTitle}">
  <jsp:attribute name="head">
    <c:if test="${useSelect2}">
      <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet"/>
      <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>
    </c:if>
    <jsp:invoke fragment="head"/>
  </jsp:attribute>
  <jsp:body>
    <header class="border-b border-gray-200 px-6 py-4">
      <h1 class="text-xl font-semibold text-gray-900">${heading}</h1>
    </header>
    <div class="px-6 py-6">
      <form action="${action}" method="POST" class="max-w-2xl space-y-5">
        <input type="hidden" name="a" value="${param.a}">
        <jsp:doBody/>
      </form>
    </div>
  </jsp:body>
</t:reportsLayout>
