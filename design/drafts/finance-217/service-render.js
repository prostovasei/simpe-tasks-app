// Рендер листов service-N.png из service-screens.html (Playwright для Node).
const { chromium } = require('playwright');
const path = require('path');
(async () => {
  const b = await chromium.launch();
  const page = await b.newPage({ viewport: { width: 1800, height: 1000 }, deviceScaleFactor: 1 });
  const errors = []; page.on('pageerror', e => errors.push(String(e)));
  await page.goto('file://' + path.join(__dirname, 'service-screens.html'));
  await page.evaluate(() => document.fonts.ready);
  const n = await page.locator('.sec').count();
  for (let i = 1; i <= n; i++) await page.locator('#s' + i).screenshot({ path: path.join(__dirname, `service-${i}.png`) });
  await b.close();
  if (errors.length) { console.error(errors); process.exit(1); }
  console.log('листов:', n);
})();
