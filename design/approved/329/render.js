// Рендер финального прототипа #329 в PNG. Запуск из корня после build.py: node design/approved/329/render.js
// Листы страницы (sheet-*.png) и снимки полноэкранного режима 1:1 на экране 412 × 860 (final-1x1*.png).
const { chromium } = require('playwright');
const path = require('node:path');

const FONT = path.resolve(__dirname, '../../../app/src/main/res/font');
const faces = [400, 500, 600, 700, 800].map((w) =>
  `@font-face { font-family: Manrope; font-weight: ${w}; src: url(file://${FONT}/manrope_${w}.ttf); }`).join('\n');
const url = 'file://' + path.join(__dirname, 'prototype.html');

async function open(browser, viewport, hash = '') {
  const page = await browser.newPage({ viewport, deviceScaleFactor: 2, colorScheme: 'light' });
  await page.goto(url + hash);
  await page.addStyleTag({ content: faces });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(300);
  return page;
}

(async () => {
  const browser = await chromium.launch();
  try {
    const page = await open(browser, { width: 1280, height: 1000 });
    for (const id of ['live', 'screens', 'widget', 'estimate']) {
      await page.locator('#' + id).screenshot({ path: path.join(__dirname, `sheet-${id}.png`) });
    }
    const phone = await open(browser, { width: 412, height: 860 }, '#full');
    await phone.screenshot({ path: path.join(__dirname, 'final-1x1.png') });
    await phone.evaluate(() => {
      const pager = document.querySelector('#full-phone .npager');
      pager.scrollLeft = pager.clientWidth + 16;
      pager.dispatchEvent(new Event('scroll'));
    });
    await phone.waitForTimeout(300);
    await phone.screenshot({ path: path.join(__dirname, 'final-1x1-news2.png') });
  } finally {
    await browser.close();
  }
})().catch((error) => { console.error(error); process.exitCode = 1; });
