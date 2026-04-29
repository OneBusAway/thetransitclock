<%
// For creating a text parameter via jsp include directive.
String label = request.getParameter("label");
String name = request.getParameter("name");
String defaultStr = request.getParameter("default");
String tooltip = request.getParameter("tooltip");
%>
<div>
  <label for="<%= name %>" class="block text-sm font-medium text-neutral-700 mb-1"><%= label %>:</label>
  <input id="<%= name %>" name="<%= name %>"
    title="<%= tooltip %>"
    value="<%= defaultStr == null ? "" : defaultStr %>"
    class="form-control w-full max-w-md"/>
</div>
