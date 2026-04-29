<%-- This file contains includes that can be included with every file --%>

<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />

<%-- Tailwind CSS, compiled by Vite (see transitclockWebapp/vite.config.js).
     Build output lands in target/frontend-dist/ and is copied into the WAR
     at /dist by maven-war-plugin's webResources. --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/dist/tailwind.css" />

<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script src="<%= request.getContextPath() %>/jquery-ui/jquery-ui.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/jquery-ui/jquery-ui.css">

<%-- Cache buster: tied to general.css mtime so edits propagate without
     forcing users to hard-refresh. getRealPath returns null for packed
     WARs, where the buster degrades to ?v=0 (still cacheable; busts
     only on redeploys that exploded the WAR). --%>
<%
String cssPath = application.getRealPath("/css/general.css");
long cssVer = cssPath != null ? new java.io.File(cssPath).lastModified() : 0L;
%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/css/general.css?v=<%= cssVer %>">

<%-- Load transitime.js after jQuery so it can override jQuery defaults. --%>
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