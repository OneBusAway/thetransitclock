<%-- This file contains includes that can be included with every file --%>

<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />

<%-- Tailwind CSS via the Play CDN. Compiles utility classes in-browser at
     load time — no build step. Fine for development and the current
     internal-tool scale; switch to a built stylesheet if perf or offline
     use becomes a concern. --%>
<script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script src="<%= request.getContextPath() %>/jquery-ui/jquery-ui.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/jquery-ui/jquery-ui.css">

<%-- Cache buster: tied to general.css mtime so edits propagate without
     forcing users to hard-refresh. Computed each request — cheap on a
     local file. getRealPath returns null for packed WARs, in which
     case the buster falls back to webapp init time. --%>
<%
String __cssPath = application.getRealPath("/css/general.css");
long __cssVer = 0L;
if (__cssPath != null) {
  java.io.File __cssFile = new java.io.File(__cssPath);
  if (__cssFile.exists()) __cssVer = __cssFile.lastModified();
}
%>

<%-- Load transitime.js after jQuery so it can override jQuery defaults. --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/css/general.css?v=<%= __cssVer %>">
<script src="<%= request.getContextPath() %>/javascript/transitime.js"></script>

<%-- Stimulus.js application entry point. ESM module, so it loads
     asynchronously after parse — fine because controllers attach on connect
     once the DOM is ready. --%>
<script type="module" src="<%= request.getContextPath() %>/javascript/application.js"></script>

<%
  String apikey = System.getProperty("transitclock.apikey");
  if (apikey == null || apikey.isEmpty()) {
    throw new IllegalStateException(
        "System property 'transitclock.apikey' is not set. " +
        "Start the JVM with -Dtransitclock.apikey=<key> before serving the webapp.");
  }
%>
<script>
var apiKey = "<%= apikey %>";
var apiUrlPrefixAllAgencies = "/api/v1/key/" + apiKey;
var apiUrlPrefix = apiUrlPrefixAllAgencies + "/agency/<%= request.getParameter("a") %>";
</script>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="${pageContext.request.locale}" />
<fmt:requestEncoding value = "UTF-8" />
<%-- The fmt resource bundle is registered app-wide in WEB-INF/web.xml as
     a jakarta.servlet.jsp.jstl.fmt.localizationContext context-param, so
     <fmt:message> works without a page-scoped <fmt:setBundle>. --%>