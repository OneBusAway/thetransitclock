<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="title"   required="true" %>
<%@ attribute name="actions" fragment="true" required="false" %>
<div class="flex h-full flex-col">
  <header class="flex shrink-0 items-center gap-4 border-b border-gray-200 bg-white px-6 py-3">
    <h1 class="text-base font-semibold text-gray-900">${title}</h1>
    <jsp:invoke fragment="actions"/>
  </header>
  <div class="relative flex-1">
    <jsp:doBody/>
  </div>
</div>
