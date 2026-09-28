/*
 * Exports every wireframe screen to docs/wireframes/png/<screen-id>.png, for the GUI
 * design section of the delivery document (docs/entregas).
 *
 * Usage (Playwright with its Chromium installed; it is not a project dependency):
 *   NODE_PATH="$(npm root -g)" node docs/wireframes/export-png.cjs [page ...]
 * With no arguments it exports every role page.
 */
const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium } = require("playwright");

const DIR = __dirname;
const PAGES = ["acceso", "super-admin", "admin", "docente", "estudiante", "evaluador-externo"];
const CHROME_UA =
  "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Safari/537.36";

/**
 * Google Fonts is fetched with curl and handed to the page, so the PNGs get the real
 * typefaces even where the browser cannot reach the internet directly (e.g. behind a
 * proxy that only curl is configured for).
 */
async function serveFontsThroughCurl(page) {
  const cache = new Map();
  await page.route(/^https:\/\/fonts\.(googleapis|gstatic)\.com\//, async (route) => {
    const url = route.request().url();
    try {
      if (!cache.has(url)) {
        cache.set(url, execFileSync("curl", ["-sSfL", "-A", CHROME_UA, url], { maxBuffer: 20 * 1024 * 1024 }));
      }
      const isCss = url.includes("fonts.googleapis.com");
      await route.fulfill({
        status: 200,
        contentType: isCss ? "text/css" : "font/woff2",
        headers: { "access-control-allow-origin": "*" },
        body: cache.get(url),
      });
    } catch (error) {
      console.warn(`font not available, using the fallback: ${url}`);
      await route.abort();
    }
  });
}

async function main() {
  const pages = process.argv.length > 2 ? process.argv.slice(2) : PAGES;
  const outDir = path.join(DIR, "png");
  fs.mkdirSync(outDir, { recursive: true });

  const browser = await chromium.launch();
  const page = await browser.newPage({
    viewport: { width: 1320, height: 900 },
    deviceScaleFactor: 1.5,
    colorScheme: "light",
  });
  await serveFontsThroughCurl(page);
  let count = 0;
  for (const name of pages) {
    const file = path.join(DIR, `${name}.html`);
    if (!fs.existsSync(file)) {
      console.warn(`skipped: ${name}.html does not exist`);
      continue;
    }
    await page.goto(`file://${file}`, { waitUntil: "networkidle" });
    await page.evaluate(() => document.fonts.ready);
    for (const screen of await page.$$(".wf-screen")) {
      const id = await screen.getAttribute("id");
      const frame = await screen.$(".frame");
      if (!id || !frame) continue;
      await frame.screenshot({ path: path.join(outDir, `${id}.png`) });
      count++;
    }
  }
  await browser.close();
  console.log(`${count} screens exported to ${path.relative(process.cwd(), outDir)}`);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
