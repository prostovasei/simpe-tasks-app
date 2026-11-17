const {chromium}=require('playwright'),fs=require('fs'),path=require('path'),assert=require('assert/strict');
(async()=>{const b=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox','--disable-dev-shm-usage']});try{
 const p=await b.newPage({viewport:{width:1120,height:1000}});const errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.setContent(fs.readFileSync(path.join(__dirname,'screens.html'),'utf8'));await p.evaluate(()=>window.layoutReady);
 assert.equal(await p.locator('.compact-day').count(),3);
 for(const s of await p.locator('.compact-day').all()){
 const a=await s.locator('.add-top').boundingBox(),c=await s.locator('.count').boundingBox();assert(a.x+a.width<=c.x);
 const styles=await s.evaluate(e=>[getComputedStyle(e.querySelector('.date-line')).fontSize,getComputedStyle(e.querySelector('.count')).fontSize]);assert.equal(styles[0],styles[1]);
 assert.equal(await s.locator('.sheet-footer').count(),0);assert(!/из/.test(await s.locator('.count').textContent()));
 await s.locator('.add-top').click();assert.match(await p.locator('.log').textContent(),/Создать задачу на ВТ 17 ноя/);
 }
 await p.locator('.log').evaluate(e=>e.style.display='none');
 await p.locator('#day .compact-day').screenshot({path:path.join(__dirname,'popup.png')});
 for(const s of await p.locator('.sec').all())await s.screenshot({path:path.join(__dirname,(await s.getAttribute('id'))+'.png')});assert.deepEqual(errors,[]);
 console.log('2 PNG: одинаковый размер даты и счётчика; плюс перед счётчиком; нижней кнопки нет; действие добавления проверено.');
}finally{await b.close()}})().catch(e=>{console.error(e);process.exitCode=1});
