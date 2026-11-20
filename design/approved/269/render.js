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
    const page = await browser.newPage({ viewport: { width: 1080, height: 960 }, deviceScaleFactor: 2 });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.setContent(fs.readFileSync(path.join(__dirname, 'screens.html'), 'utf8'));
    await page.evaluate(() => document.fonts.ready);

    const tasks = page.locator('#tasks');
    const chooser = page.locator('#chooser');
    const finance = page.locator('#finance');
    const taskTab = tasks.getByRole('tab', { name: 'Задачи' });
    const financeTab = tasks.getByRole('tab', { name: 'Финансы' });
    assert.equal(await tasks.getByRole('tab').count(), 2);
    assert.equal(await taskTab.getAttribute('aria-selected'), 'true');
    assert.equal(await finance.getByRole('tab', { name: 'Финансы' }).getAttribute('aria-selected'), 'true');

    await chooser.getByRole('tab', { name: 'Задачи' }).click();
    assert(await chooser.getByRole('dialog').isVisible());
    assert.deepEqual(await chooser.getByRole('dialog').locator('button span').allTextContents(), ['Все', 'Входящие', 'Неделя']);
    assert.equal(await chooser.locator('[data-mode="week"]').getAttribute('aria-pressed'), 'true');
    await page.locator('#standard').screenshot({ path: path.join(__dirname, 'widget-tabs.png') });
    await finance.locator('.widget-frame').screenshot({ path: path.join(__dirname, 'widget-finance.png') });

    for (const stage of [tasks, chooser, finance, ...await page.locator('.compact').all()]) {
      const barFits = await stage.locator('.widget-bar').evaluate(bar => {
        const buttons = [...bar.querySelectorAll('button')].filter(button => !button.hidden);
        const edges = bar.getBoundingClientRect();
        return bar.scrollWidth <= bar.clientWidth && buttons.every(button => {
          const rect = button.getBoundingClientRect();
          return rect.left >= edges.left && rect.right <= edges.right;
        });
      });
      assert(barFits, 'Нижняя панель помещается в виджет');
    }

    await tasks.getByRole('button', { name: 'Новая задача' }).click();
    assert.equal(await tasks.getAttribute('data-last-action'), 'task');
    await taskTab.click();
    await tasks.getByRole('button', { name: 'Входящие' }).click();
    assert(await tasks.locator('[data-view="inbox"]').isVisible());
    await financeTab.click();
    assert(await tasks.locator('.finance-pane').isVisible());
    await tasks.getByRole('button', { name: 'Новая операция' }).click();
    assert.equal(await tasks.getAttribute('data-last-action'), 'operation');
    await taskTab.click();
    assert(await tasks.locator('[data-view="inbox"]').isVisible());
    assert(!(await tasks.getByRole('dialog').isVisible()));
    await taskTab.click();
    assert.equal(await tasks.locator('[data-mode="inbox"]').getAttribute('aria-pressed'), 'true');
    await page.keyboard.press('Escape');
    assert(!(await tasks.getByRole('dialog').isVisible()));
    await taskTab.click();
    await tasks.getByRole('button', { name: 'Все', exact: true }).click();
    assert(await tasks.locator('[data-view="all"]').isVisible());
    assert.equal(await chooser.getAttribute('data-mode'), 'week');
    assert.equal(await finance.getAttribute('data-module'), 'finance');

    const accounts = finance.locator('.widget-accounts li');
    assert.equal(await accounts.count(), 6);
    assert.match(await finance.locator('.amount-actual').innerText(), /28k\s*₽/);
    assert.match(await finance.locator('.amount-estimate').innerText(), /45k\s*₽/);
    assert.equal(await finance.locator('.forecast').count(), 1);
    assert.deepEqual(await finance.locator('.pace > svg .date-tick, .pace > svg .current-date').allTextContents(), ['01', '19', '30']);
    for (const key of ['categories', 'places']) {
      const table = finance.locator('.' + key);
      assert.equal(await table.locator('tbody tr').count(), 5);
      const amounts = await table.locator('tbody td').allTextContents();
      assert.equal(amounts.reduce((sum, value) => sum + Number(value.replace(/\s/g, '')), 0), 28400);
    }
    const categories = await finance.locator('.categories').boundingBox();
    const places = await finance.locator('.places').boundingBox();
    assert(categories.x + categories.width <= places.x && categories.y === places.y);

    const small = page.locator('#compact-chart .finance-pane');
    assert(await small.evaluate(pane => {
      const chart = pane.querySelector('.pace > svg').getBoundingClientRect();
      return pane.scrollHeight > pane.clientHeight && pane.scrollWidth <= pane.clientWidth &&
        chart.bottom <= pane.getBoundingClientRect().bottom;
    }), 'В компактном виджете график виден сразу, остальное прокручивается');
    for (const [id, table] of [['compact-ranking', '.categories'], ['compact-places', '.places']]) {
      await page.locator(`#${id} .finance-pane`).evaluate((pane, selector) => {
        pane.scrollTop = pane.querySelector(selector).getBoundingClientRect().top - pane.getBoundingClientRect().top;
      }, table);
    }
    const smallAccounts = page.locator('#compact-accounts .finance-pane');
    const pinnedBar = await page.locator('#compact-accounts .widget-bar').boundingBox();
    await smallAccounts.evaluate(pane => { pane.scrollTop = pane.scrollHeight; });
    assert(await smallAccounts.evaluate(pane => {
      const last = pane.querySelector('.widget-accounts li:last-child').getBoundingClientRect();
      const visible = pane.getBoundingClientRect();
      return last.top >= visible.top && last.bottom <= visible.bottom;
    }), 'Последний счёт доступен в компактном виджете');
    assert.equal((await page.locator('#compact-accounts .widget-bar').boundingBox()).y, pinnedBar.y);
    await page.locator('#compact').screenshot({ path: path.join(__dirname, 'widget-compact.png') });
    assert.deepEqual(errors, []);
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
