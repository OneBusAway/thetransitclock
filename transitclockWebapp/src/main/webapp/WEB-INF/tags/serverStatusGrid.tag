<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ attribute name="monitorResults" required="false" type="java.util.List" %>
<%@ attribute name="error"          required="false" %>
<c:choose>
  <c:when test="${not empty error}">
    <div class="rounded-lg border border-red-200 bg-red-50 p-4">
      <h3 class="text-sm font-semibold text-red-900"><fmt:message key="div.coreUnreachable"/></h3>
      <p class="mt-1 text-sm text-red-800 font-mono break-all"><c:out value="${error}"/></p>
    </div>
  </c:when>
  <c:otherwise>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <c:forEach var="monitorResult" items="${monitorResults}">
        <c:if test="${not empty monitorResult.message}">
          <article class="bg-white border border-gray-200 rounded-lg p-4">
            <h3 class="text-xs font-semibold uppercase tracking-wider text-gray-500">
              <c:out value="${monitorResult.type}"/>
            </h3>
            <c:choose>
              <c:when test="${not empty monitorResult.stats}">
                <dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-1.5 text-sm">
                  <c:forEach var="stat" items="${monitorResult.stats}">
                    <div class="flex items-baseline gap-1.5 col-span-2 sm:col-span-1">
                      <dt class="text-gray-500"><c:out value="${stat.key}"/>:</dt>
                      <dd class="font-mono text-gray-900"><c:out value="${stat.value}"/></dd>
                    </div>
                  </c:forEach>
                </dl>
              </c:when>
              <c:otherwise>
                <p class="mt-2 text-sm text-gray-900 leading-relaxed tabular-nums">
                  <c:out value="${monitorResult.message}"/>
                </p>
              </c:otherwise>
            </c:choose>
          </article>
        </c:if>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
