import { Controller } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";

const SUMMARY_REFRESH_MS = 60_000;

// Source of truth: OneBusAway design package (separate repo) at
// project/assets/colors_and_type.css — red = early, green = on time,
// violet = late.
const STATUS_STYLES = Object.freeze({
  early:  { text: "text-status-early",       bg: "bg-status-early/10",   solid: "bg-status-early"   },
  onTime: { text: "text-status-on-time-ink", bg: "bg-status-on-time/10", solid: "bg-status-on-time" },
  late:   { text: "text-status-late",        bg: "bg-status-late/10",    solid: "bg-status-late"    },
});
const ALL_ADH_CLASSES = Object.values(STATUS_STYLES).flatMap((s) => [s.text, s.bg]);
const ALL_STRIPE_CLASSES = [...Object.values(STATUS_STYLES).map((s) => s.solid), "bg-gray-200"];

const blocksLabel = (n) => `${n} ${n === 1 ? "block" : "blocks"}`;

export default class extends Controller {
  static targets = ["summary", "asOf", "accordion", "routeTemplate", "blockTemplate", "loadAll"];
  static values = {
    earlyMsec: Number,
    lateMsec: Number,
  };

  connect() {
    this.#fetchRoutes();
    this.#fetchSummary();
    this.summaryTimer = setInterval(() => this.#fetchSummary(), SUMMARY_REFRESH_MS);
  }

  disconnect() {
    clearInterval(this.summaryTimer);
  }

  routeOpened(event) {
    const item = event.detail?.item;
    if (item) this.#fetchRoute(item);
  }

  async loadAll() {
    const button = this.hasLoadAllTarget ? this.loadAllTarget : null;
    if (button) button.disabled = true;
    try {
      const items = Array.from(this.accordionTarget.querySelectorAll("[data-accordion-target='item']"));
      await Promise.all(items.map((item) => this.#fetchRoute(item)));
    } finally {
      if (button) button.disabled = false;
    }
  }

  async #fetchRoutes() {
    const data = await this.#getJSON(`${window.apiUrlPrefix}/command/activeBlocksByRouteWithoutVehicles`,
                                     "activeBlocksByRouteWithoutVehicles");
    if (data) this.#renderRoutes(data);
  }

  async #fetchSummary() {
    const params = new URLSearchParams({
      allowableEarlySec: String(this.earlyMsecValue / 1000),
      allowableLateSec: String(this.lateMsecValue / 1000),
    });
    const data = await this.#getJSON(`${window.apiUrlPrefix}/command/vehicleAdherenceSummary?${params}`,
                                     "vehicleAdherenceSummary");
    if (data) this.#renderSummary(data);
  }

  async #fetchRoute(item) {
    const routeName = item.dataset.routeName;
    if (!routeName) return;
    const url = `${window.apiUrlPrefix}/command/activeBlockByRouteNameWithVehicles?r=${encodeURIComponent(routeName)}`;
    const data = await this.#getJSON(url, `activeBlockByRouteNameWithVehicles for ${routeName}`);
    if (!data) return;
    const route = data.routes?.find((r) => r.id === item.dataset.routeId) ?? data.routes?.[0];
    if (route) this.#renderRouteData(item, route);
  }

  async #getJSON(url, label) {
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      return await res.json();
    } catch (err) {
      console.error(`${label} failed`, err);
      return null;
    }
  }

  #renderSummary(total) {
    const blocks = total.blocks ?? 0;
    const late = total.late ?? 0;
    const onTime = total.ontime ?? 0;
    const early = total.early ?? 0;
    const assigned = late + onTime + early;
    const pct = (n) => (blocks ? `${((100 * n) / blocks).toFixed(0)}%` : "—");

    this.#fill("total-blocks", blocks);
    this.#fill("percent-late", pct(late));
    this.#fill("percent-on-time", pct(onTime));
    this.#fill("percent-early", pct(early));
    this.#fill("late-count", late ? blocksLabel(late) : "");
    this.#fill("on-time-count", onTime ? blocksLabel(onTime) : "");
    this.#fill("early-count", early ? blocksLabel(early) : "");
    this.#fill("assigned-detail", blocks ? `${assigned}/${blocks}` : "");

    if (this.hasAsOfTarget) this.asOfTarget.textContent = new Date().toLocaleTimeString();

    const assignedEl = this.summaryTarget.querySelector("[data-field='percent-assigned']");
    if (assignedEl) {
      assignedEl.textContent = pct(assigned);
      const assignedFrac = blocks ? assigned / blocks : 1;
      // Drop the brand-accent color and switch to red when assignment is
      // unhealthy, so the at-a-glance signal still reads correctly.
      assignedEl.classList.toggle("text-brand-accent", assignedFrac >= 0.9);
      assignedEl.classList.toggle("text-status-early", assignedFrac < 0.9);
    }
  }

  #fill(field, value) {
    const el = this.summaryTarget.querySelector(`[data-field='${field}']`);
    if (el) el.textContent = String(value);
  }

  #renderRoutes(data) {
    const routes = data.routes ?? [];
    const accordion = this.accordionTarget;
    accordion.querySelectorAll(":scope > :not([data-route-id])").forEach((el) => el.remove());
    const existing = new Map();
    accordion.querySelectorAll("[data-route-id]").forEach((el) => existing.set(el.dataset.routeId, el));
    const seen = new Set();

    for (const route of routes) {
      seen.add(route.id);
      let item = existing.get(route.id);
      if (!item) {
        const fragment = this.routeTemplateTarget.content.cloneNode(true);
        item = fragment.querySelector("[data-accordion-target='item']");
        item.dataset.routeId = route.id;
        item.dataset.routeName = route.name;
        accordion.appendChild(item);
      }
      const blockCount = route.block?.length ?? 0;
      item.querySelector("[data-field='route-tag']").textContent = route.id;
      item.querySelector("[data-field='route-name']").textContent = route.name;
      item.querySelector("[data-field='route-blocks']").textContent = blockCount;
      item.querySelector("[data-field='route-blocks-noun']").textContent = blockCount === 1 ? "block" : "blocks";
      item.querySelector("[data-field='route-blocks-denom']").textContent = blockCount;
    }

    for (const [id, el] of existing) {
      if (!seen.has(id)) el.remove();
    }
  }

  #renderRouteData(item, route) {
    let early = 0;
    let onTime = 0;
    let late = 0;
    for (const block of route.block ?? []) {
      for (const v of block.vehicle ?? []) {
        if (v.scheduleBased) continue;
        const kind = this.#classifyAdh(parseInt(v.schAdh, 10));
        if (kind === "early") early++;
        else if (kind === "late") late++;
        else onTime++;
      }
    }
    const vehicleTotal = early + onTime + late;
    const blockCount = (route.block ?? []).length;

    item.querySelector("[data-field='route-early']").textContent = early;
    item.querySelector("[data-field='route-on-time']").textContent = onTime;
    item.querySelector("[data-field='route-late']").textContent = late;
    item.querySelector("[data-field='route-blocks']").textContent = blockCount;
    item.querySelector("[data-field='route-blocks-denom']").textContent = blockCount;
    item.querySelector("[data-field='route-blocks-noun']").textContent = blockCount === 1 ? "block" : "blocks";

    // Vehicle count below block count means the route is short-staffed;
    // surface that as red regardless of the per-vehicle adherence colors.
    const vehEl = item.querySelector("[data-field='route-vehicles']");
    vehEl.textContent = vehicleTotal;
    vehEl.classList.toggle("text-status-early", vehicleTotal < blockCount);

    this.#toggleVisible(item, "route-early-pair", early > 0);
    this.#toggleVisible(item, "route-on-time-pair", onTime > 0);
    this.#toggleVisible(item, "route-late-pair", late > 0);

    const summary = item.querySelector("[data-vehicle-summary]");
    summary?.classList.remove("hidden");
    summary?.classList.add("flex");

    // Stripe priority is late > early > on-time, falling back to neutral
    // gray when no vehicles report yet — otherwise every unloaded card
    // would mis-signal "all on time".
    const stripe = item.querySelector("[data-field='route-stripe']");
    if (stripe) {
      stripe.classList.remove(...ALL_STRIPE_CLASSES);
      const kind = late > 0 ? "late" : early > 0 ? "early" : onTime > 0 ? "onTime" : null;
      stripe.classList.add(kind ? STATUS_STYLES[kind].solid : "bg-gray-200");
    }

    const blockList = item.querySelector("[data-block-list]");
    blockList.replaceChildren();
    for (const block of route.block ?? []) {
      blockList.appendChild(this.#renderBlock(block));
    }
  }

  #toggleVisible(item, field, visible) {
    const el = item.querySelector(`[data-field='${field}']`);
    if (!el) return;
    el.classList.toggle("hidden", !visible);
    el.classList.toggle("inline-flex", visible);
  }

  #classifyAdh(schAdh) {
    if (schAdh < -this.lateMsecValue) return "late";
    if (schAdh > this.earlyMsecValue) return "early";
    return "onTime";
  }

  #applyAdhClasses(el, kind) {
    el.classList.remove(...ALL_ADH_CLASSES);
    const style = STATUS_STYLES[kind];
    el.classList.add(style.text, style.bg);
  }

  #renderBlock(block) {
    const fragment = this.blockTemplateTarget.content.cloneNode(true);
    const row = fragment.firstElementChild;

    row.querySelector("[data-field='block-id']").textContent = block.id ?? "";
    row.querySelector("[data-field='block-start']").textContent = block.startTime ?? "";
    row.querySelector("[data-field='block-end']").textContent = block.endTime ?? "";
    row.querySelector("[data-field='block-service']").textContent = block.serviceId ?? "";
    row.querySelector("[data-field='trip-id']").textContent = block.trip?.shortName ?? block.trip?.id ?? "";
    row.querySelector("[data-field='trip-start']").textContent = block.trip?.startTime ?? "";
    row.querySelector("[data-field='trip-end']").textContent = block.trip?.endTime ?? "";
    row.querySelector("[data-field='trip-headsign']").textContent = block.trip?.headsign ?? "";

    const vehiclesEl = row.querySelector("[data-field='block-vehicles']");
    const adhEl = row.querySelector("[data-field='block-sch-adh']");
    const vehicles = block.vehicle ?? [];
    if (vehicles.length === 0) {
      vehiclesEl.textContent = "—";
      adhEl.textContent = "—";
      adhEl.classList.add("text-gray-500");
      return row;
    }

    vehiclesEl.textContent = vehicles.map((v) => v.id).join(", ");
    const v0 = vehicles[0];
    const kind = this.#classifyAdh(parseInt(v0.schAdh, 10));
    adhEl.replaceChildren();
    const dot = document.createElement("span");
    dot.className = `inline-block size-2 rounded-full ${STATUS_STYLES[kind].solid}`;
    adhEl.appendChild(dot);
    adhEl.appendChild(document.createTextNode(v0.schAdhStr ?? "—"));
    this.#applyAdhClasses(adhEl, kind);
    return row;
  }
}
