<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="language" value="" scope="session" />
<fmt:setLocale value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}" />
<fmt:requestEncoding value = "UTF-8" />
<fmt:setBundle basename="org.transitclock.i18n.text" />
<% 
// For creating a boolean parameter via jsp include directive.

// Get the parameters for this boolean parameter
String label = request.getParameter("label"); 
String name = request.getParameter("name"); 
String defaultStr = request.getParameter("default");
boolean defaultValue = defaultStr != null && defaultStr.toLowerCase().equals("false") ? 
	false : true;
String tooltipStr = request.getParameter("tooltip");
if (tooltipStr == null)
    tooltipStr = "";
%>

  <div class="param">
     <label for="<%= name %>"><%= label %>:</label> 
     <select id="<%= name %>" name="<%= name %>" title="<%= tooltipStr %>">
       <option value="true"  <%=  defaultValue ? "selected=\"selected\"" : ""%>><fmt:message key="div.true" /></option>
       <option value="false" <%= !defaultValue ? "selected=\"selected\"" : ""%>><fmt:message key="div.false" /></option>
     </select>
   </div>
