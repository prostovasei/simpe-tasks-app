// Рендер листов прототипа #329 в PNG. Запуск из корня после build.py: node design/approved/329/render.js
const { chromium } = require('playwright');
const path = require('node:path');

const FONT = path.resolve(__dirname, '../../../app/src/main/res/font');
const faces = [400, 500, 600, 700, 800].map((w) =>
  `@font-face { font-family: Manrope; font-weight: ${w}; src: url(file://${FONT}/manrope_${w}.ttf); }`).join('\n');

(async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1280, height: 1000 }, deviceScaleFactor: 2, colorScheme: 'light' });
    await page.goto('file://' + path.join(__dirname, 'prototype.html'));
    await page.addStyleTag({ content: faces });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    for (const id of ['live', 'density', 'nav', 'extras', 'widget', 'estimate']) {
      await page.locator('#' + id).screenshot({ path: path.join(__dirname, `sheet-${id}.png`) });
    }
  } finally {
    await browser.close();
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
