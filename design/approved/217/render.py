from pathlib import Path
from playwright.sync_api import sync_playwright
import base64
root=Path(__file__).parent
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
 page=b.new_page(viewport={'width':1160,'height':960},device_scale_factor=1.5)
 errors=[]
 page.on('pageerror',lambda e:errors.append(str(e)))
 page.set_content((root/'compact-module-bar.html').read_text())
 page.evaluate('document.fonts.ready')
 page.locator('#phone .module-bar [data-module=tasks]').click()
 assert page.locator('#phone header').count()==0
 page.locator('#phone').screenshot(path=str(root/'compact-module-bar-tasks.png'))
 page.locator('#phone .module-bar [data-module=finance]').click()
 assert page.locator('#phone .account-tile').count()==3
 page.locator('#phone').screenshot(path=str(root/'compact-module-bar-finance.png'))
 page.locator('#phone [data-tab=operations]').click()
 page.locator('#phone .module-bar [data-module=tasks]').click()
 page.locator('#phone .module-bar [data-module=finance]').click()
 assert page.locator('#phone [data-tab=operations]').get_attribute('class')=='nav active'
 assert not errors,errors
 cards=''
 for file,label in [('tasks','Задачи'),('finance','Финансы')]:
  data=base64.b64encode((root/f'compact-module-bar-{file}.png').read_bytes()).decode()
  cards+=f'<article><h2>{label}</h2><img src="data:image/png;base64,{data}"></article>'
 page.set_viewport_size({'width':900,'height':980})
 page.set_content('<html><meta charset="utf-8"><style>body{background:#e8e8e8;font-family:Arial;margin:0;padding:28px;color:#17171c}h1{font-size:24px;margin:0 0 10px}p{font-size:13px;color:#666;margin-bottom:20px}.grid{display:flex;gap:28px}h2{font-size:17px}img{width:390px;border-radius:28px}</style><h1>Компактный бар без заголовков</h1><p>Иконки слева · активный модуль подсвечен · настройки финансов справа</p><div class="grid">'+cards+'</div>')
 page.screenshot(path=str(root/'compact-module-bar-preview.png'),full_page=True)
 b.close()
 print('Rendered both modules; switching and finance tab preservation checked.')
