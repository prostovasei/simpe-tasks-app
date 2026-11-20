const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');

async function renderApp() {
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM_PATH || '/usr/bin/chromium',
    args: ['--no-sandbox'],
  });
  try {
    const page = await browser.newPage({ viewport: { width: 360, height: 850 }, deviceScaleFactor: 2 });
    await page.setContent(fs.readFileSync(path.join(__dirname, 'finance-app.html'), 'utf8'));
    await page.addStyleTag({ content: 'body { margin: 0; background: #f5f5f6; }' });
    await page.addScriptTag({ path: require.resolve('lucide/dist/umd/lucide.min.js') });
    await page.evaluate(() => lucide.createIcons());
    await page.locator('.finance-screen').screenshot({ path: path.join(__dirname, 'finance-app.png') });
  } finally {
    await browser.close();
  }
}

renderApp().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
