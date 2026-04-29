<%@ tag pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- The five-stat strip shown on /status/activeBlocks.jsp and /dashboard.
     Both pages mount the active-blocks Stimulus controller; the data-field
     names here are the controller's #renderSummary contract. Keep that
     contract here in one place so callers can't drift. --%>
<div data-active-blocks-target="summary"
     class="flex items-stretch bg-white border border-gray-200 rounded-lg overflow-hidden shadow-[0_1px_2px_rgba(0,0,0,0.03)]">
  <div class="flex-1 min-w-0 px-4 py-3 border-r border-gray-200">
    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.blocks"/></div>
    <div class="flex items-baseline gap-1.5 mt-0.5">
      <span data-field="total-blocks" class="text-[22px] font-bold tabular-nums text-gray-900">—</span>
      <span class="text-xs text-gray-500 font-medium">total</span>
    </div>
  </div>
  <div class="flex-1 min-w-0 px-4 py-3 border-r border-gray-200">
    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.assigned"/></div>
    <div class="flex items-baseline gap-1.5 mt-0.5">
      <span data-field="percent-assigned" class="text-[22px] font-bold tabular-nums text-brand-accent">—</span>
      <span data-field="assigned-detail" class="text-xs text-gray-500 font-medium tabular-nums"></span>
    </div>
  </div>
  <div class="flex-1 min-w-0 px-4 py-3 border-r border-gray-200">
    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.contime"/></div>
    <div class="flex items-baseline gap-1.5 mt-0.5">
      <span data-field="percent-on-time" class="text-[22px] font-bold tabular-nums text-status-on-time-ink">—</span>
      <span data-field="on-time-count" class="text-xs text-gray-500 font-medium tabular-nums"></span>
    </div>
  </div>
  <div class="flex-1 min-w-0 px-4 py-3 border-r border-gray-200">
    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.clate"/></div>
    <div class="flex items-baseline gap-1.5 mt-0.5">
      <span data-field="percent-late" class="text-[22px] font-bold tabular-nums text-status-late">—</span>
      <span data-field="late-count" class="text-xs text-gray-500 font-medium tabular-nums"></span>
    </div>
  </div>
  <div class="flex-1 min-w-0 px-4 py-3">
    <div class="text-[11px] font-semibold uppercase tracking-wider text-gray-500"><fmt:message key="div.cearly"/></div>
    <div class="flex items-baseline gap-1.5 mt-0.5">
      <span data-field="percent-early" class="text-[22px] font-bold tabular-nums text-status-early">—</span>
      <span data-field="early-count" class="text-xs text-gray-500 font-medium tabular-nums"></span>
    </div>
  </div>
</div>
