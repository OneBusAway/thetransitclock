import { Controller } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";

const SUMMARY_REFRESH_MS = 60_000;

const ADH_CLASSES = Object.freeze({
  early:  ["text-amber-700",   "bg-amber-50"],
  onTime: ["text-emerald-700", "bg-emerald-50"],
  late:   ["text-red-700",     "bg-red-50"],
});
const ALL_ADH_CLASSES = [...new Set(Object.values(ADH_CLASSES).flat())];

export default class extends Controller {
  static targets = ["summary", "accordion", "routeTemplate", "blockTemplate", "loadAll"];
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
    try {
      const res = await fetch(`${window.apiUrlPrefix}/command/activeBlocksByRouteWithoutVehicles`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.#renderRoutes(await res.json());
    } catch (err) {
      console.error("activeBlocksByRouteWithoutVehicles failed", err);
    }
  }

  async #fetchSummary() {
    const params = new URLSearchParams({
      allowableEarlySec: String(this.earlyMsecValue / 1000),
      allowableLateSec: String(this.lateMsecValue / 1000),
    });
    try {
      const res = await fetch(`${window.apiUrlPrefix}/command/vehicleAdherenceSummary?${params}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.#renderSummary(await res.json());
    } catch (err) {
      console.error("vehicleAdherenceSummary failed", err);
    }
  }

  async #fetchRoute(item) {
    const routeName = item.dataset.routeName;
    if (!routeName) return;
    const url = `${window.apiUrlPrefix}/command/activeBlockByRouteNameWithVehicles?r=${encodeURIComponent(routeName)}`;
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const route = data.routes?.find((r) => r.id === item.dataset.routeId) ?? data.routes?.[0];
      if (route) this.#renderRouteData(item, route);
    } catch (err) {
      console.error(`activeBlockByRouteNameWithVehicles failed for ${routeName}`, err);
    }
  }

  #renderSummary(total) {
    const blocks = total.blocks ?? 0;
    const vehicleCount = (total.late ?? 0) + (total.ontime ?? 0) + (total.early ?? 0);
    const pct = (n) => (blocks ? `${((100 * n) / blocks).toFixed(0)}%` : "—");

    this.#fill("total-blocks", blocks);
    this.#fill("percent-late", pct(total.late ?? 0));
    this.#fill("percent-on-time", pct(total.ontime ?? 0));
    this.#fill("percent-early", pct(total.early ?? 0));
    this.#fill("as-of", new Date().toLocaleTimeString());

    const assignedEl = this.summaryTarget.querySelector("[data-field='percent-assigned']");
    if (assignedEl) {
      assignedEl.textContent = pct(vehicleCount);
      const assignedFrac = blocks ? vehicleCount / blocks : 1;
      assignedEl.classList.toggle("text-red-600", assignedFrac < 0.9);
    }
  }

  #fill(field, value) {
    const el = this.summaryTarget.querySelector(`[data-field='${field}']`);
    if (el) el.textContent = String(value);
  }

  #renderRoutes(data) {
    const routes = data.routes ?? [];
    const accordion = this.accordionTarget;
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
      item.querySelector("[data-field='route-name']").textContent = route.name;
      item.querySelector("[data-field='route-blocks']").textContent = route.block?.length ?? 0;
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

    const earlyEl   = item.querySelector("[data-field='route-early']");
    const onTimeEl  = item.querySelector("[data-field='route-on-time']");
    const lateEl    = item.querySelector("[data-field='route-late']");
    const vehEl     = item.querySelector("[data-field='route-vehicles']");
    const blocksEl  = item.querySelector("[data-field='route-blocks']");

    earlyEl.textContent  = early;
    onTimeEl.textContent = onTime;
    lateEl.textContent   = late;
    vehEl.textContent    = vehicleTotal;
    blocksEl.textContent = blockCount;

    earlyEl.classList.toggle("text-amber-700", early > 0);
    earlyEl.classList.toggle("bg-amber-50",    early > 0);
    lateEl.classList.toggle("text-red-700",    late > 0);
    lateEl.classList.toggle("bg-red-50",       late > 0);
    const understaffed = vehicleTotal < blockCount;
    vehEl.classList.toggle("text-red-700",     understaffed);
    vehEl.classList.toggle("bg-red-50",        understaffed);

    const summary = item.querySelector("[data-vehicle-summary]");
    summary?.classList.remove("hidden");
    summary?.classList.add("flex");

    const blockList = item.querySelector("[data-block-list]");
    blockList.replaceChildren();
    for (const block of route.block ?? []) {
      blockList.appendChild(this.#renderBlock(block));
    }
  }

  #classifyAdh(schAdh) {
    if (schAdh < -this.lateMsecValue) return "late";
    if (schAdh > this.earlyMsecValue) return "early";
    return "onTime";
  }

  #applyAdhClasses(el, schAdh) {
    el.classList.remove(...ALL_ADH_CLASSES);
    el.classList.add(...ADH_CLASSES[this.#classifyAdh(schAdh)]);
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
    adhEl.textContent = v0.schAdhStr ?? "—";
    this.#applyAdhClasses(adhEl, v0.schAdh);
    return row;
  }
}
