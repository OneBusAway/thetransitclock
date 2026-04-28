<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="message" required="false" %>
<div class="flex flex-col items-center justify-center gap-3 p-8 text-neutral-500 dark:text-neutral-400">
  <svg xmlns="http://www.w3.org/2000/svg" class="rounded-full animate-spin size-8" viewBox="0 0 18 18" aria-hidden="true">
    <g fill="currentColor">
      <path d="m9,17c-4.4111,0-8-3.5889-8-8S4.5889,1,9,1s8,3.5889,8,8-3.5889,8-8,8Zm0-14.5c-3.584,0-6.5,2.916-6.5,6.5s2.916,6.5,6.5,6.5,6.5-2.916,6.5-6.5-2.916-6.5-6.5-6.5Z" opacity=".4" stroke-width="0"/>
      <path d="m16.25,9.75c-.4141,0-.75-.3359-.75-.75,0-3.584-2.916-6.5-6.5-6.5-.4141,0-.75-.3359-.75-.75s.3359-.75.75-.75c4.4111,0,8,3.5889,8,8,0,.4141-.3359.75-.75.75Z" stroke-width="0"/>
    </g>
  </svg>
  <p class="text-sm font-medium" role="status" aria-live="polite">
    <c:choose>
      <c:when test="${not empty message}">${message}</c:when>
      <c:otherwise><fmt:message key="div.loading"/></c:otherwise>
    </c:choose>
  </p>
</div>
