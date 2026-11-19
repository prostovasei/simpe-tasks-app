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
    for (const kind of ['categories', 'places']) {
      const table = overview.locator('table.' + kind);
      assert.equal(await table.locator('tbody tr').count(), 5);
      assert.deepEqual(await table.locator('tbody .index').allInnerTexts(), ['1', '2', '3', '4', '5']);
      assert.match(await table.locator('tbody tr').last().innerText(), /Остальное/);
      assert.match(await table.locator('tbody tr').last().innerText(), kind === 'categories' ? /1\s*900/ : /8\s*900/);
      assert.equal(await table.locator('thead .currency').innerText(), '₽');
      const figures = await table.locator('tbody td').allInnerTexts();
      assert(figures.every(value => /^[\d\s]+$/.test(value)));
      assert.equal(figures.reduce((sum, value) => sum + Number(value.replace(/\s/g, '')), 0), 28400);
    }
    assert.equal(await overview.locator('.shares,.segment,.unassigned').count(), 0);
    const pace = overview.locator('.pace');
    assert.equal(await pace.getAttribute('data-days'), '30');
    assert.equal(await pace.getAttribute('data-actual-days'), '19');
    assert.match(await pace.locator('.amount-actual').innerText(), /^28k\s₽$/);
    assert.match(await pace.locator('.amount-remaining').innerText(), /^12k\s₽$/);
    assert.match(await pace.locator('.amount-estimate').innerText(), /^45k\s₽$/);
    assert.equal(await pace.locator('.pace-summary svg').count(), 3);
    assert.deepEqual(await pace.locator('.date-tick, .current-date').allTextContents(), ['01', '19', '30']);
    assert.equal(await pace.locator('.current-date').textContent(), '19');
    const coordinates = await pace.evaluate(panel => {
      const line = panel.querySelector('.trend');
      const end = line.points.getItem(line.points.numberOfItems - 1);
      const forecast = panel.querySelector('.forecast');
      const lastDate = panel.querySelector('.date-tick:last-child');
      const cutoff = panel.querySelector('.cutoff');
      const currentDate = panel.querySelector('.current-date');
      return { from: Number(forecast.getAttribute('x1')), to: Number(forecast.getAttribute('x2')),
        actualX: end.x, actualDays: line.points.numberOfItems, dash: getComputedStyle(forecast).strokeDasharray,
        lastDateX: Number(lastDate.getAttribute('x')), todayX: Number(currentDate.getAttribute('x')),
        cutoffX: Number(cutoff.getAttribute('x1')) };
    });
    assert.equal(coordinates.actualDays, 19);
    assert(Math.abs(coordinates.from - coordinates.actualX) < 0.01);
    assert.equal(coordinates.to, coordinates.lastDateX);
    assert.equal(coordinates.from, coordinates.todayX);
    assert.equal(coordinates.cutoffX, coordinates.todayX);
    assert.notEqual(coordinates.dash, 'none');
    const completed = page.locator('#completed .pace');
    assert.equal(await completed.getAttribute('data-days'), '31');
    assert.equal(await completed.getAttribute('data-actual-days'), '31');
    assert.equal(await completed.locator('.forecast').count(), 0);
    assert.equal(await completed.locator('.amount-estimate').count(), 0);
    const scales = page.locator('#scale-examples');
    const charts = [pace, ...await scales.locator('.pace').all()];
    const limits = [50000, 100000, 150000];
    assert.equal(charts.length, limits.length);
    for (const [index, chart] of charts.entries()) {
      const limit = limits[index];
      const labels = await chart.locator('.value-tick').allTextContents();
      assert.deepEqual(labels, [`${limit / 1000}k`, `${limit / 2000}k`]);
      const pathData = await chart.locator('.grid').getAttribute('d');
      const guides = [...pathData.matchAll(/M\d+ (\d+)H\d+/g)].map(match => Number(match[1]));
      assert.deepEqual(guides, [8, 77, 146]);
      const middleLabelY = Number(await chart.locator('.value-tick').nth(1).getAttribute('y'));
      assert(middleLabelY > guides[1] && middleLabelY < guides[2]);
      const datePositions = await chart.locator('.date-tick, .current-date').evaluateAll(labels =>
        labels.map(label => Number(label.getAttribute('y'))));
      assert(datePositions.every(position => position > guides.at(-1)), 'Даты находятся под графиком');
    }
    await scales.screenshot({ path: path.join(__dirname, 'chart-scales.png') });
    await overview.screenshot({ path: path.join(__dirname, 'month.png') });
    await overview.locator('.rankings').screenshot({ path: path.join(__dirname, 'rankings.png') });
    await page.setViewportSize({ width: 320, height: 1050 });
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    const largestSummary = charts.at(-1).locator('.pace-summary');
    const amountsFit = await largestSummary.evaluate(summary => {
      const [actual, remaining, forecast] = Array.from(summary.children, child => child.getBoundingClientRect());
      return summary.scrollWidth <= summary.clientWidth && actual.y === remaining.y && remaining.y === forecast.y &&
        actual.right < remaining.left && remaining.right < forecast.left;
    });
    assert(amountsFit, 'Три суммы с иконками помещаются в одну строку при ширине 320 px');
    const categoryBox = await overview.locator('table.categories').boundingBox();
    const placeBox = await overview.locator('table.places').boundingBox();
    assert(categoryBox.x + categoryBox.width <= placeBox.x, 'Категории слева от мест');
    assert.equal(categoryBox.y, placeBox.y, 'Две таблицы стоят в одной строке');
    for (const kind of ['categories', 'places']) {
      const rowsFit = await overview.locator('table.' + kind + ' tbody tr').evaluateAll(rows => rows.every(row => {
        const title = row.querySelector('.stat-title');
        return title.scrollWidth <= title.clientWidth && row.scrollWidth <= row.clientWidth;
      }));
      assert(rowsFit, 'Текст и суммы помещаются при ширине 320 px');
    }
    await overview.locator('.rankings').screenshot({ path: path.join(__dirname, 'rankings-narrow.png') });
    const missing = page.locator('#missing .places tbody');
    assert.equal(await missing.locator('tr').count(), 1);
    assert.equal(await missing.locator('.index').innerText(), '1');
    assert.match(await missing.innerText(), /Остальное\s+28\s*400/);
    await page.locator('#missing').screenshot({ path: path.join(__dirname, 'without-places.png') });

    const finance = page.locator('#finance');
    assert.match(await finance.locator('.daily-average').innerText(), /^1,5k\s+₽\s*\/день$/);
    assert.equal(await finance.locator('.month-selector span').innerText(), 'ноя 26');
    assert(await finance.evaluate(screen => screen.scrollWidth <= screen.clientWidth));
    await page.setViewportSize({ width: 400, height: 900 });
    await page.addStyleTag({ content: 'main { display: block; padding: 0 } main > :not(#finance) { display: none }' });
    const accountsBox = await finance.locator('.accounts').boundingBox();
    const monthBox = await finance.locator('.expenses-heading').boundingBox();
    const chartBox = await finance.locator('.pace').boundingBox();
    const rankingsBox = await finance.locator('.rankings').boundingBox();
    const barBox = await finance.locator('.finance-bar').boundingBox();
    assert(accountsBox.y + accountsBox.height <= monthBox.y && monthBox.y + monthBox.height <= chartBox.y);
    assert(chartBox.y + chartBox.height <= rankingsBox.y && rankingsBox.y + rankingsBox.height <= barBox.y);
    assert.equal(await finance.locator('.finance-tabs [aria-current="page"]').getAttribute('aria-label'), 'Аналитика');
    assert.equal((await finance.boundingBox()).height, 900);
    await finance.screenshot({ path: path.join(__dirname, 'finance-home.png') });
    assert.deepEqual(errors, []);
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
