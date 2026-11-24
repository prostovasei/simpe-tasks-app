// Рендер макетов #327: каждый лист в PNG. Запуск из корня после screens.py: node design/approved/327/render.js
const { chromium } = require('playwright');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1200, height: 1400 }, deviceScaleFactor: 2 });
    await page.goto('file://' + path.join(__dirname, 'screens.html'));
    await page.evaluate(() => document.fonts.ready);
    for (const id of ['sheet-1', 'sheet-2', 'sheet-3', 'sheet-4']) {
      await page.locator('#' + id).screenshot({ path: path.join(__dirname, id + '.png') });
    }
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
