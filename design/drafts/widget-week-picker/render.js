const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

async function render() {
  const local = name => path.join(__dirname, name);
  const read = name => fs.readFileSync(local(name), "utf8");
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_PATH || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 500, height: 750 }, deviceScaleFactor: 3 });
    await page.setContent(read("screens.html")
      .replace('<link rel="stylesheet" href="screen.css">', () => "<style>" + read("screen.css") + "</style>")
      .replace('<script src="preview.js"></script>', () => "<script>" + read("preview.js") + "</script>"));
    await page.evaluate(() => document.fonts.ready);
    for (const option of await page.locator(".variant").all()) {
      assert.equal(await option.locator(".week").count(), 6);
      assert.equal(await option.locator(".date").count(), 42);
      assert.equal(await option.locator("[aria-current=date]").textContent(), "21");
      assert.equal(await option.locator("[aria-pressed=true]").getAttribute("aria-label"), "16.11 – 22.11");
      assert.ok(await option.locator(".date").evaluateAll(dates => dates.every(day => {
        const style = getComputedStyle(day);
        const current = day.getAttribute("aria-current") === "date";
        return style.fontWeight === (current ? "700" : "400") && style.backgroundColor === "rgba(0, 0, 0, 0)";
      })));
    }
    await page.locator(".board").screenshot({ path: local("variants.png") });
    await page.setViewportSize({ width: 364, height: 750 });
    for (const option of await page.locator(".variant").all()) {
      assert.ok(await option.locator(".week").evaluateAll(rows => rows.every(row =>
        row.children.length === 7 && row.firstElementChild.getBoundingClientRect().width >= 20,
      )));
      await option.getByRole("button", { name: "09.11 – 15.11" }).click();
      assert.equal(await option.locator("[aria-pressed=true]").getAttribute("aria-label"), "09.11 – 15.11");
      assert.equal(await option.locator("[aria-current=date]").textContent(), "21");
      assert.equal(await option.locator(".sunday-title span").textContent(), "15");
    }
  } finally {
    await browser.close();
  }
}

render().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
