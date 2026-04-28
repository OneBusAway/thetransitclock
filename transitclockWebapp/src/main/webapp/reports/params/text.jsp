<%
// For creating a text parameter via jsp include directive.
String label = request.getParameter("label");
String name = request.getParameter("name");
String defaultStr = request.getParameter("default");
String tooltip = request.getParameter("tooltip");
%>
<div>
  <label for="<%= name %>" class="block text-sm font-medium text-gray-900 mb-1"><%= label %>:</label>
  <input id="<%= name %>" name="<%= name %>"
    title="<%= tooltip %>"
    value="<%= defaultStr == null ? "" : defaultStr %>"
    class="block w-full max-w-md rounded-md border-0 py-1.5 px-3 text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 focus:ring-2 focus:ring-inset focus:ring-indigo-600 sm:text-sm sm:leading-6"/>
</div>
