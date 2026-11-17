// Каркас approved/241/render.js. Только рендер/проверка HTML-макетов #259.
const {chromium} = require('playwright');
const path = require('path');
const fs = require('fs');
const assert = require('assert/strict');
(async () => {
  const browser = await chromium.launch({executablePath: process.env.CHROMIUM_PATH || '/usr/bin/chromium', args: ['--no-sandbox', '--disable-dev-shm-usage']});
  try {
    const page = await browser.newPage({viewport: {width: 1120, height: 1100}, deviceScaleFactor: 1});
    const errors = [];
    page.on('pageerror', e => errors.push(String(e)));
    page.on('requestfailed', r => errors.push(r.url() + ': ' + r.failure()?.errorText));
    await page.setContent(fs.readFileSync(path.join(__dirname, 'screens.html'), 'utf8'));
    await page.evaluate(() => window.layoutReady);
    assert.equal(await page.locator('[id^="variant-"]').count(), 10);
    assert.equal(await page.locator('.sec').count(), 15);
    const geometry = await page.evaluate(() => {
      const samples = [...document.querySelectorAll('[id^="variant-"]')];
      return samples.map(s => [...s.querySelectorAll('.device')].map(d => {
        const t = d.querySelector('.task'); const tm=t.querySelector('.time'); const n=t.querySelector('.node'); const title=t.querySelector('.title');
        return {row:t.getBoundingClientRect().height, time:tm.getBoundingClientRect().width, node:n.getBoundingClientRect().width,
          gap1:n.getBoundingClientRect().left-tm.getBoundingClientRect().right,
          gap2:title.getBoundingClientRect().left-n.getBoundingClientRect().right,
          font:getComputedStyle(title).fontSize, count:d.querySelectorAll('.task').length};
      }));
    });
    for (const pair of geometry) {
      assert.deepEqual(pair, geometry[0], 'Вариант изменил геометрию/вместимость');
      for(const g of pair){ assert.equal(g.node,14); assert.equal(g.gap1,2); assert.equal(g.gap2,2); }
    }
    // Нажатия должны оставаться независимыми; макет только сообщает выбранное действие.
    const root = page.locator('#interactions .app');
    for (const [selector, text] of [['.day:nth-child(2) .task:nth-child(1) .toggle', 'Выполнить задачу'],
      ['.day:nth-child(1) .task:nth-child(5) .toggle', 'Вернуть задачу'],
      ['.day:nth-child(1) .task:nth-child(3) .time', 'Выполнить задачу'],
      ['.day:nth-child(3) .task:nth-child(1) .toggle', 'Открыть исходную задачу'],
      ['.day:nth-child(2) .task:nth-child(1) .title', 'Открыть задачу'],
      ['.day:nth-child(4) .blank', 'Открыть день']]) {
      await root.locator(selector).click();
      assert.match(await page.locator('.action-log').textContent(), new RegExp('^'+text));
    }
    await page.locator('.action-log').evaluate(el => el.classList.remove('on'));
    await page.locator('.nav').evaluate(el => el.style.position = 'static');
    for (const section of await page.locator('.sec').all()) {
      const id=await section.getAttribute('id');
      await section.screenshot({path:path.join(__dirname, id+'.png')});
    }
    assert.deepEqual(errors, []);
    console.log('15 PNG; 10 вариантов: одинаковые размеры, 14 dp / 2 dp / 2 dp; зоны нажатий HTML проверены.');
  } finally { await browser.close(); }
})().catch(e => {console.error(e); process.exitCode=1;});
