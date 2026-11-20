const {chromium}=require('playwright');
const fs=require('node:fs');
const assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});try{
const page=await browser.newPage({viewport:{width:1120,height:1400},deviceScaleFactor:2});
await page.setContent(fs.readFileSync(__dirname+'/screens.html','utf8'));
const compact = page.locator('.screen.small');
assert.equal(await compact.locator('.control').count(), 3);
assert.equal(await compact.locator('.control[aria-label="Обновить"], .date').count(), 0);
assert.ok(await compact.locator('.bar').evaluate(bar => bar.offsetWidth === 180 && bar.offsetHeight === 40 &&
  [...bar.querySelectorAll('.control,.add')].every(item => item.getBoundingClientRect().width === 42)));
assert.equal(await page.locator('#app .screen .tabs .control').count(), 20);
for(const id of ['app','widget']) await page.locator('#'+id).screenshot({path:__dirname+'/'+id+'.png'});
}finally{await browser.close()}})().catch(e=>{console.error(e);process.exitCode=1});
