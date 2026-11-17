// Листы на основе Playwright-рендера approved/259/render.js.
const {chromium} = require('playwright');
const fs = require('fs');
const path = require('path');
const assert = require('assert/strict');
(async () => {
 const b = await chromium.launch({executablePath: process.env.CHROMIUM_PATH || '/usr/bin/chromium', args: ['--no-sandbox','--disable-dev-shm-usage']});
 try {
  const p = await b.newPage({viewport:{width:1120,height:1100},deviceScaleFactor:1});
  const errors=[]; p.on('pageerror',e=>errors.push(String(e))); p.on('requestfailed',r=>errors.push(r.url()));
  await p.setContent(fs.readFileSync(path.join(__dirname,'screens.html'),'utf8'));
  await p.evaluate(()=>window.layoutReady);
  assert.equal(await p.locator('[id^="variant-"]').count(),20);
  assert.equal(await p.locator('#baseline').count(),1);
  assert.equal(await p.locator('[id^="overview-"]').count(),4);
  const dims=await p.evaluate(()=>Array.from(document.querySelectorAll('[id^="variant-"] .device'),d=>{
   const row=d.querySelector('.task');const time=row.querySelector('.time');const node=row.querySelector('.node');const title=row.querySelector('.title');
   const a=time.getBoundingClientRect(), b=node.getBoundingClientRect(), c=title.getBoundingClientRect();
   return {device:d.classList.contains('widget')?'widget':'app',height:row.getBoundingClientRect().height,node:b.width,gap1:b.left-a.right,gap2:c.left-b.right};
  }));
  for(const x of dims){assert.equal(x.node,14);assert.equal(x.gap1,2);assert.equal(x.gap2,2)}
  for(const group of ['app','widget']){const a=dims.filter(x=>x.device===group);for(const v of a) assert.deepEqual(v,a[0]);}
  await p.locator('.nav').evaluate(el=>el.style.position='static');
  for(const s of await p.locator('.sec').all()) await s.screenshot({path:path.join(__dirname,(await s.getAttribute('id'))+'.png')});
  assert.deepEqual(errors,[]);console.log('25 PNG: 20 вариантов, 4 сравнения и текущий вид; геометрия строк проверена.');
 } finally {await b.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
