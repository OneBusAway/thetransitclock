<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="title" required="true" %>
<%-- Middle-column sidebar shell shared by reports and API calls index pages.
     The body should contain section <div>s with their own headings/links;
     this tag only owns the wrapper, title row, and scroll container. --%>
<aside class="hidden md:flex md:flex-col w-64 lg:w-72 shrink-0 border-r border-gray-200 bg-gray-50">
  <div class="px-5 py-4 border-b border-gray-200">
    <h2 class="text-base font-semibold text-gray-900">${title}</h2>
  </div>
  <nav class="flex-1 overflow-y-auto px-3 py-4 space-y-6">
    <jsp:doBody/>
  </nav>
</aside>
