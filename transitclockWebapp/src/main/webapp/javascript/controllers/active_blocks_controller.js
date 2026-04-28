import { Controller } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";

const SUMMARY_REFRESH_MS = 60_000;

export default class extends Controller {
  static targets = ["summary", "accordion", "routeTemplate", "blockTemplate", "loadAll"];
  static values = {
    earlyMsec: Number,
    lateMsec: Number,
    apiPrefix: String,
  };

  connect() {
    this.#fetchRoutes();
    this.#fetchSummary();
    this.summaryTimer = setInterval(() => this.#fetchSummary(), SUMMARY_REFRESH_MS);
  }

  disconnect() {
    clearInterval(this.summaryTimer);
  }

  // Stimulus action — invoked from data-action="accordion:opened->active-blocks#routeOpened".
  routeOpened(event) {
    const item = event.detail?.item;
    if (item) this.#fetchRoute(item);
  }

  async loadAll() {
    const button = this.hasLoadAllTarget ? this.loadAllTarget : null;
    if (button) button.disabled = true;
    try {
      for (const item of this.accordionTarget.querySelectorAll("[data-accordion-target='item']")) {
        await this.#fetchRoute(item);
      }
    } finally {
      if (button) button.disabled = false;
    }
  }

  async #fetchRoutes() {
    try {
      const res = await fetch(`${this.apiPrefixValue}/command/activeBlocksByRouteWithoutVehicles`);
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
      const res = await fetch(`${this.apiPrefixValue}/command/vehicleAdherenceSummary?${params}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      this.#renderSummary(await res.json());
    } catch (err) {
      console.error("vehicleAdherenceSummary failed", err);
    }
  }

  async #fetchRoute(item) {
    const routeName = item.dataset.routeName;
    if (!routeName) return;
    const url = `${this.apiPrefixValue}/command/activeBlockByRouteNameWithVehicles?r=${encodeURIComponent(routeName)}`;
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
    this.#fill("percent-assigned", blocks ? `${((100 * vehicleCount) / blocks).toFixed(0)}%` : "—");
    this.#fill("percent-late", pct(total.late ?? 0));
    this.#fill("percent-on-time", pct(total.ontime ?? 0));
    this.#fill("percent-early", pct(total.early ?? 0));
    this.#fill("as-of", new Date().toLocaleTimeString());

    const assignedPct = blocks ? (100 * vehicleCount) / blocks : 100;
    const assignedEl = this.summaryTarget.querySelector("[data-field='percent-assigned']");
    assignedEl?.classList.toggle("text-red-600", assignedPct < 90);
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
        const adh = parseInt(v.schAdh, 10);
        if (adh < -this.lateMsecValue) late++;
        else if (adh > this.earlyMsecValue) early++;
        else onTime++;
      }
    }
    const vehicleTotal = early + onTime + late;
    const blockCount = (route.block ?? []).length;

    item.querySelector("[data-field='route-early']").textContent = early;
    item.querySelector("[data-field='route-on-time']").textContent = onTime;
    item.querySelector("[data-field='route-late']").textContent = late;
    item.querySelector("[data-field='route-vehicles']").textContent = vehicleTotal;
    item.querySelector("[data-field='route-blocks']").textContent = blockCount;
    const summary = item.querySelector("[data-vehicle-summary]");
    summary?.classList.remove("hidden");
    summary?.classList.add("flex");

    this.#chip(item.querySelector("[data-field='route-early']"), early > 0, "amber");
    this.#chip(item.querySelector("[data-field='route-late']"), late > 0, "red");
    this.#chip(item.querySelector("[data-field='route-vehicles']"), vehicleTotal < blockCount, "red");

    const blockList = item.querySelector("[data-block-list]");
    blockList.replaceChildren();
    for (const block of route.block ?? []) {
      blockList.appendChild(this.#renderBlock(block));
    }
  }

  #chip(el, on, color) {
    if (!el) return;
    const text = `text-${color}-700`;
    const bg = `bg-${color}-50`;
    el.classList.toggle(text, on);
    el.classList.toggle(bg, on);
  }

  #renderBlock(block) {
    const fragment = this.blockTemplateTarget.content.cloneNode(true);
    const row = fragment.firstElementChild;

    row.querySelector("[data-field='block-id']").textContent = block.id ?? "";
    row.querySelector("[data-field='block-start']").textContent = block.startTime ?? "";
    row.querySelector("[data-field='block-end']").textContent = block.endTime ?? "";
    row.querySelector("[data-field='block-service']").textContent = block.serviceId ?? "";

    const tripId = block.trip?.shortName ?? block.trip?.id ?? "";
    row.querySelector("[data-field='trip-id']").textContent = tripId;
    row.querySelector("[data-field='trip-start']").textContent = block.trip?.startTime ?? "";
    row.querySelector("[data-field='trip-end']").textContent = block.trip?.endTime ?? "";
    row.querySelector("[data-field='trip-headsign']").textContent = block.trip?.headsign ?? "";

    const vehicles = block.vehicle ?? [];
    const vehiclesEl = row.querySelector("[data-field='block-vehicles']");
    const adhEl = row.querySelector("[data-field='block-sch-adh']");
    if (vehicles.length === 0) {
      vehiclesEl.textContent = "—";
      adhEl.textContent = "—";
      adhEl.classList.add("text-gray-500");
      return row;
    }

    vehiclesEl.textContent = vehicles.map((v) => v.id).join(", ");
    const v0 = vehicles[0];
    adhEl.textContent = v0.schAdhStr ?? "—";
    if (v0.schAdh < -this.lateMsecValue) adhEl.classList.add("text-red-700", "bg-red-50");
    else if (v0.schAdh > this.earlyMsecValue) adhEl.classList.add("text-amber-700", "bg-amber-50");
    else adhEl.classList.add("text-emerald-700", "bg-emerald-50");

    return row;
  }
}
