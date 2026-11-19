const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

(async () => {
  const browser = await chromium.launch({
    executablePath: '/usr/bin/chromium',
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 460, height: 1050 }, deviceScaleFactor: 2 });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.setContent(fs.readFileSync(path.join(__dirname, 'screens.html'), 'utf8'));
    await page.evaluate(() => document.fonts.ready);
    const overview = page.locator('#overview');
    const top = overview.locator('.ranking').first();
    assert.equal(await top.locator('li').count(), 5);
    assert.equal(await overview.locator('.share').count(), 5);
    assert.match(await overview.locator('.unassigned').innerText(), /5\s*900\s*₽/);
    await overview.locator('summary').click();
    assert(await overview.getByText('Кофейня').isVisible());
    await overview.locator('summary').click();
    assert(!(await overview.getByText('Кофейня').isVisible()));
    await overview.screenshot({ path: path.join(__dirname, 'month.png') });
    await overview.locator('.places').screenshot({ path: path.join(__dirname, 'places.png') });
    await page.setViewportSize({ width: 320, height: 1050 });
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    assert.match(await page.locator('#missing .unassigned').innerText(), /28\s*400\s*₽/);
    await page.locator('#missing').screenshot({ path: path.join(__dirname, 'without-places.png') });
    assert.deepEqual(errors, []);
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
