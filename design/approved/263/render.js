const {chromium}=require('playwright');
const fs=require('fs'),path=require('path'),assert=require('assert/strict');
(async()=>{
 const b=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox','--disable-dev-shm-usage']});
 try{
 const p=await b.newPage({viewport:{width:1120,height:1100},deviceScaleFactor:1});
 const errors=[];p.on('pageerror',e=>errors.push(String(e)));
 await p.setContent(fs.readFileSync(path.join(__dirname,'screens.html'),'utf8'));
 await p.evaluate(()=>window.layoutReady);
 assert.equal(await p.locator('.sec').count(),5);
 assert.equal(await p.locator('.now,.handle').count(),0);
 for(const d of await p.locator('#week .device').all()){
 assert.equal(await d.locator('.week-cell').count(),7);
 assert.equal(await d.locator('.week-cell .toggle-node,.week-cell .node').count(),0);
 const cell=d.locator('.week-cell').nth(1);
 for(const target of ['.day-head','.title','.wtime','.list-icon','.cell-space']){
 await cell.locator(target).first().click({force:true});
 assert.match(await p.locator('.log').textContent(),/Открыть день: ВТ 17/);
 }
 }
 for(const panel of await p.locator('.sample-panel').all()){
 const heights=await panel.locator('.timeline-row').evaluateAll(es=>es.map(e=>e.getBoundingClientRect().height));
 assert.deepEqual(heights,[46,46,46,46]);
 const nodes=await panel.locator('.toggle-node').evaluateAll(es=>es.map(e=>[e.getBoundingClientRect().width,e.getBoundingClientRect().height]));
 for(const n of nodes)assert.deepEqual(n,nodes[0]);
 }
 await p.locator('.log').evaluate(e=>e.style.display='none');
 await p.locator('.nav').evaluate(e=>e.style.position='static');
 for(const s of await p.locator('.sec').all())await s.screenshot({path:path.join(__dirname,(await s.getAttribute('id'))+'.png')});
 assert.deepEqual(errors,[]);console.log('5 PNG; зоны ячеек и равная геометрия периодов проверены.');
 }finally{await b.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
