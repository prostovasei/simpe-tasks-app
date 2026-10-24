import { chromium } from '/opt/node-tools/node_modules/playwright/index.mjs';
const b = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium' }).catch(()=>chromium.launch());
const p = await b.newPage({ viewport: { width: +(process.env.W||1360), height: +(process.env.H||1040) } });
const fs = await import('fs');
for (const k of process.env.KS.split(',')) {
  let s = fs.readFileSync(`${process.env.DIR}/Variant${k}.dc.html`,'utf8');
  s = s.split('<x-dc>')[1].split('</x-dc>')[0];
  await p.setContent(`<body style="margin:0">${s}</body>`);
  await p.evaluate(() => document.fonts.ready);
  await p.screenshot({ path: `${process.env.DIR}/shot_${k}.png` });
}
await b.close();
