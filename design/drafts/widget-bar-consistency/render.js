const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

async function render() {
  const read = name => fs.readFileSync(path.join(__dirname, name), "utf8");
  const html = read("screens.html")
    .replace('<link rel="stylesheet" href="screen.css">', () => "<style>" + read("screen.css") + "</style>")
    .replace('<script src="preview.js"></script>', () => "<script>" + read("preview.js") + "</script>");
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_PATH || "/usr/bin/chromium",
    args: ["--no-sandbox"],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 520, height: 720 }, deviceScaleFactor: 3 });
    await page.setContent(html);
    await page.evaluate(() => document.fonts.ready);
    for (const [width, gaps] of [
      [392, [[72, 52], [75, 57], [78, 64]]],
      [320, [[36, 16], [39, 21], [42, 28]]],
    ]) {
      await page.locator("#width").selectOption(String(width));
      const boxes = await page.locator(".widget").evaluateAll(widgets => widgets.map(widget => {
        const rectangle = selector => widget.querySelector(selector).getBoundingClientRect();
        const widgetBox = widget.getBoundingClientRect();
        const tabs = rectangle(".tabs");
        const left = rectangle(".gap.left");
        const right = rectangle(".gap.right");
        return {
          width: widgetBox.width,
          center: tabs.left + tabs.width / 2 - widgetBox.left,
          gaps: [left.width, right.width],
          contiguous: rectangle(".add").right === left.left && left.right === tabs.left &&
            tabs.right === right.left && right.right === rectangle(".date").left,
          gapHeight: left.height === rectangle(".date").height && right.height === rectangle(".date").height,
        };
      }));
      assert.deepEqual(boxes, gaps.map(dimensions => ({
        width, center: width / 2, gaps: dimensions, contiguous: true, gapHeight: true,
      })));
    }

    await page.locator("#width").selectOption("392");
    assert.ok(await page.locator(".bar svg path").evaluateAll(paths =>
      paths.every(path => Math.abs(path.getBoundingClientRect().height + 1.8 - 16) < 0.15),
    ));
    assert.ok(await page.locator(".date").first().evaluate(date => {
      const paint = document.createElement("canvas").getContext("2d");
      const metrics = node => {
        const font = getComputedStyle(node);
        paint.font = [font.fontWeight, font.fontSize, font.fontFamily].join(" ");
        return paint.measureText(node.textContent);
      };
      const number = metrics(date.querySelector(".date-number"));
      const top = metrics(date.querySelector(".date-caption span"));
      const bottom = metrics(date.querySelector(".date-caption span:last-child"));
      const captionHeight = top.actualBoundingBoxAscent + 9.5 + bottom.actualBoundingBoxDescent;
      return Math.abs(number.actualBoundingBoxAscent + number.actualBoundingBoxDescent - captionHeight) <= 1.5;
    }));
    await page.locator(".board").screenshot({ path: path.join(__dirname, "variants.png") });
    if (process.argv.includes("--approved")) {
      await page.locator(".choice").first().screenshot({ path: path.join(__dirname, "../../approved/282/bar.png") });
    }
    await page.locator("#zones").check();
    await page.locator(".board").screenshot({ path: path.join(__dirname, "hitboxes.png") });
    if (process.argv.includes("--approved")) {
      await page.locator(".choice").first().screenshot({ path: path.join(__dirname, "../../approved/282/hitboxes.png") });
    }

    for (const choice of await page.locator(".choice").all()) {
      const status = choice.locator("output");
      for (const gap of await choice.locator(".gap").all()) {
        await gap.click();
        assert.equal(await status.textContent(), await gap.getAttribute("aria-label"));
      }
      await choice.getByRole("tab", { name: "Финансы" }).click();
      assert.equal(await status.textContent(), "Финансы");
      assert.ok(await choice.locator(".date").isDisabled());
      await choice.getByRole("tab", { name: "Задачи" }).click();
      await choice.locator(".date").click();
      assert.equal(await status.textContent(), "Вернуться к сегодня");
      await choice.locator(".add").click();
      assert.equal(await status.textContent(), "Добавить");
      await choice.locator(".refresh").click();
      assert.equal(await status.textContent(), "Обновление");
    }
    await page.locator("#width").selectOption("240");
    const compact = page.locator(".widget").first();
    assert.equal(await compact.locator(".gap.left").evaluate(gap => gap.getBoundingClientRect().width), 60);
    assert.equal(await compact.locator(".gap.right:visible, .date:visible").count(), 0);
    await compact.locator(".gap.left").click();
    assert.equal(await page.locator(".choice").first().locator("output").textContent(), "Открыть приложение, левый промежуток");
    await page.locator("#width").selectOption("180");
    assert.equal(await page.locator(".bar .gap:visible, .bar .date:visible, .bar .refresh:visible").count(), 0);
    assert.equal(await page.locator(".bar .tab:visible").count(), 9);
    assert.deepEqual(await page.locator(".widget").first().locator(".add, .tab").evaluateAll(
      nodes => nodes.map(node => node.getBoundingClientRect().width),
    ), [48, 40, 40, 40]);
  } finally {
    await browser.close();
  }
}

render().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
