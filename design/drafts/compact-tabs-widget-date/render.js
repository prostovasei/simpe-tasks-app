const assert = require("node:assert/strict");
const path = require("node:path");
const fs = require("node:fs");
const { chromium } = require("playwright");

async function render() {
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_PATH || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  try {
    const page = await browser.newPage({
      viewport: { width: 840, height: 1800 },
      deviceScaleFactor: 2,
    });
    const base = file => path.join(__dirname, file);
    const html = fs.readFileSync(base("screens.html"), "utf8")
      .replace('<link rel="stylesheet" href="screen.css">', () =>
        "<style>" + fs.readFileSync(base("screen.css"), "utf8") + "</style>")
      .replace(/src="(\.\.\/\.\.\/\.\.\/app\/screenshots\/[a-z0-9_]+\.png)"/g,
        (_, name) => 'src="data:image/png;base64,' + fs.readFileSync(base(name)).toString("base64") + '"');
    await page.setContent(html, { waitUntil: "load" });
    await page.evaluate(() => document.fonts.ready);
    const brokenImages = await page.locator("img").evaluateAll(
      images => images.filter(image => !image.complete || !image.naturalWidth).map(image => image.src),
    );
    assert.deepEqual(brokenImages, []);

    const positions = await page.locator(".wide").evaluateAll(widgets => widgets.map(widget => {
      const start = widget.getBoundingClientRect().left;
      return [...widget.querySelectorAll(".tabs button, .date, .update")].map(node => {
        const box = node.getBoundingClientRect();
        return [box.left - start, box.width];
      });
    }));
    assert.equal(positions.length, 3);
    for (const position of positions) {
      assert.deepEqual(position, [[100, 40], [140, 40], [180, 40], [220, 50], [270, 44]]);
    }
    assert.equal(await page.locator(".wide button.date").count(), 1);
    assert.equal(await page.locator(".wide span.date").count(), 2);
    const dateHeights = await page.locator(".wide .date").first().evaluate(date => {
      const canvas = document.createElement("canvas").getContext("2d");
      function glyphs(element) {
        const style = getComputedStyle(element);
        canvas.font = [style.fontWeight, style.fontSize, style.fontFamily].join(" ");
        return canvas.measureText(element.textContent);
      }
      const day = glyphs(date.querySelector(".day"));
      const week = glyphs(date.querySelector(".date-label b"));
      const month = glyphs(date.querySelector(".date-label span"));
      const lineHeight = parseFloat(getComputedStyle(date.querySelector(".date-label")).lineHeight);
      return [day.actualBoundingBoxAscent + day.actualBoundingBoxDescent,
        week.actualBoundingBoxAscent + lineHeight + month.actualBoundingBoxDescent];
    });
    assert.ok(Math.abs(dateHeights[0] - dateHeights[1]) <= 2, dateHeights.join(" / "));

    const compact = page.locator(".compact");
    assert.equal(await compact.locator(".date, .update").count(), 0);
    assert.deepEqual(await compact.locator(".bar button").evaluateAll(
      buttons => buttons.map(button => button.getBoundingClientRect().width),
    ), [48, 40, 40, 40]);
    assert.equal(await page.locator(".settings-screen .bar").count(), 0);
    assert.deepEqual(await page.locator(".app-tasks .tabs button").evaluateAll(
      buttons => buttons.map(button => button.getBoundingClientRect().width),
    ), [48, 48, 48, 48]);

    for (const section of ["app", "widget"]) {
      await page.locator("#" + section).screenshot({ path: path.join(__dirname, section + ".png") });
    }
  } finally {
    await browser.close();
  }
}

render().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
