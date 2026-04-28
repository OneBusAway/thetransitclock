import { Controller } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";

export default class extends Controller {
  static targets = ["panel", "chevron"];

  toggle(event) {
    event.preventDefault();
    const expanded = this.panelTarget.classList.toggle("hidden") === false;
    this.element.querySelector("[aria-expanded]")?.setAttribute("aria-expanded", expanded);
    if (this.hasChevronTarget) {
      this.chevronTarget.classList.toggle("rotate-90", expanded);
    }
  }
}
