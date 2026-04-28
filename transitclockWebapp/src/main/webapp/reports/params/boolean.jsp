<%
// For creating a boolean parameter via jsp include directive.
String label = request.getParameter("label");
String name = request.getParameter("name");
String defaultStr = request.getParameter("default");
boolean defaultValue = !(defaultStr != null && defaultStr.equalsIgnoreCase("false"));
String tooltipStr = request.getParameter("tooltip");
if (tooltipStr == null) tooltipStr = "";
%>
<div>
  <label for="<%= name %>" class="block text-sm font-medium text-gray-900 mb-1"><%= label %>:</label>
  <select id="<%= name %>" name="<%= name %>" title="<%= tooltipStr %>"
    class="block w-full max-w-xs rounded-md border-0 py-1.5 pl-3 pr-10 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6">
    <option value="true"  <%=  defaultValue ? "selected=\"selected\"" : "" %>><fmt:message key="div.true"/></option>
    <option value="false" <%= !defaultValue ? "selected=\"selected\"" : "" %>><fmt:message key="div.false"/></option>
  </select>
</div>
