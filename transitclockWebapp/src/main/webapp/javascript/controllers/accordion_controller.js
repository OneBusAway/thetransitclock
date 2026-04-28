import { Controller } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";

export default class extends Controller {
  static targets = ["item"];
  static values = { allowMultiple: Boolean };

  toggle(event) {
    const item = event.currentTarget.closest("[data-accordion-target='item']");
    if (!item) return;
    const isOpen = item.dataset.state === "open";

    if (!this.allowMultipleValue && !isOpen) {
      this.itemTargets.forEach((other) => {
        if (other !== item) this.#close(other);
      });
    }

    if (isOpen) {
      this.#close(item);
    } else {
      this.#open(item);
    }
  }

  #open(item) {
    const trigger = item.querySelector("[data-accordion-target='trigger']");
    const content = item.querySelector("[data-accordion-target='content']");
    item.dataset.state = "open";
    trigger.dataset.state = "open";
    trigger.setAttribute("aria-expanded", "true");
    content.dataset.state = "open";
    content.removeAttribute("hidden");
    content.querySelector("[data-state]")?.setAttribute("data-state", "open");
    this.dispatch("opened", { detail: { item }, target: item });
  }

  #close(item) {
    const trigger = item.querySelector("[data-accordion-target='trigger']");
    const content = item.querySelector("[data-accordion-target='content']");
    item.dataset.state = "closed";
    trigger.dataset.state = "closed";
    trigger.setAttribute("aria-expanded", "false");
    content.dataset.state = "closed";
    content.setAttribute("hidden", "");
    content.querySelector("[data-state]")?.setAttribute("data-state", "closed");
  }
}
