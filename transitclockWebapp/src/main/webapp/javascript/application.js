// Stimulus app entry point. Loaded as <script type="module"> from
// template/includes.jsp, so it runs in every page that renders the layout.
// Add new controllers by importing them here and calling Stimulus.register.
import { Application } from "https://unpkg.com/@hotwired/stimulus@3.2.2/dist/stimulus.js";
import DisclosureController from "./controllers/disclosure_controller.js";
import AccordionController from "./controllers/accordion_controller.js";
import ActiveBlocksController from "./controllers/active_blocks_controller.js";

window.Stimulus = Application.start();
Stimulus.register("disclosure", DisclosureController);
Stimulus.register("accordion", AccordionController);
Stimulus.register("active-blocks", ActiveBlocksController);
