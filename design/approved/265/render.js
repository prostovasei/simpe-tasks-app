const {chromium}=require('playwright'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
(async()=>{const b=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const p=await b.newPage({viewport:{width:1120,height:1000}});const errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.setContent(fs.readFileSync(path.join(__dirname,'screens.html'),'utf8'));await p.evaluate(()=>window.layoutReady);
 assert.equal(await p.locator('.compact-day').count(),3);
 for(const s of await p.locator('.compact-day').all()){
 assert.equal(await s.locator('.count').count(),0);
 assert.equal(await s.locator('.date-line').textContent(),'ВТ 17 ноя');
 assert(!/Сегодня|Вчера|Завтра/.test(await s.locator('.sheet-header').textContent()));
 const a=await s.locator('.add-top i').boundingBox();assert.equal(a.width,64);assert.equal(a.height,32);
 assert.equal(await s.locator('.sheet-footer').count(),0);
 await s.locator('.add-top').click();assert.match(await p.locator('.log').textContent(),/Создать задачу на ВТ 17 ноя/);
 }
 await p.locator('.log').evaluate(e=>e.style.display='none');
 await p.locator('#day .compact-day').screenshot({path:path.join(__dirname,'popup.png')});
 for(const s of await p.locator('.sec').all())await s.screenshot({path:path.join(__dirname,(await s.getAttribute('id'))+'.png')});assert.deepEqual(errors,[]);
 console.log('PNG: только дата, без относительных подписей и количества; плюс 64×32; нижней кнопки нет; действие добавления проверено.');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
