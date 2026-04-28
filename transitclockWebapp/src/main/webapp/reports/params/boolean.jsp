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
  <label for="<%= name %>" class="block text-sm font-medium text-neutral-700 mb-1"><%= label %>:</label>
  <select id="<%= name %>" name="<%= name %>" title="<%= tooltipStr %>"
    class="form-control w-full max-w-xs">
    <option value="true"  <%=  defaultValue ? "selected=\"selected\"" : "" %>><fmt:message key="div.true"/></option>
    <option value="false" <%= !defaultValue ? "selected=\"selected\"" : "" %>><fmt:message key="div.false"/></option>
  </select>
</div>
