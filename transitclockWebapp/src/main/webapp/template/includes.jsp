<%-- This file contains includes that can be included with every file --%>

<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />

<%-- Tailwind CSS via the Play CDN. Compiles utility classes in-browser at
     load time — no build step. Fine for development and the current
     internal-tool scale; switch to a built stylesheet if perf or offline
     use becomes a concern. --%>
<script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>

<%-- Load in JQuery --%>
<script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>

<%-- Load in JQuery UI javascript and css to set general look and feel, such as for tooltips --%>
<script src="<%= request.getContextPath() %>/jquery-ui/jquery-ui.js"></script>
<link rel="stylesheet" href="<%= request.getContextPath() %>/jquery-ui/jquery-ui.css">
  
<%-- Load in Transitime css and javascript libraries. Do this after jquery files
     loaded so can override those parameters as necessary. --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/css/general.css">
<script src="<%= request.getContextPath() %>/javascript/transitime.js"></script>

<script>
// This needs to match the API key in the database
//var apiKey = "f78a2e9a"
 var apiKey="<%=System.getProperty("transitclock.apikey")%>"
// For accessing the api for an agency command
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