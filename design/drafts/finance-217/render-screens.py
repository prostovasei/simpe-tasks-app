from pathlib import Path
from playwright.sync_api import sync_playwright
import base64

root=Path(__file__).parent
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
 page=b.new_page(viewport={'width':1160,'height':950},device_scale_factor=1.5,locale='ru-RU')
 errors=[]
 page.on('pageerror',lambda err:errors.append(str(err)))
 page.set_content((root/'finance-screens.html').read_text(),wait_until='load')
 page.evaluate('document.fonts.ready')
 for scene in ['analytics','operations','accounts','expense','keyboard','transfer','income','newaccount','empty']:
  page.evaluate('(s)=>showScene(s)',scene)
  page.locator('#phone').screenshot(path=str(root/f'{scene}.png'))
  if scene in ['expense','keyboard','transfer','income','newaccount']:
   button=page.locator('.form-actions .primary').bounding_box()
   dialog=page.locator('.dialog').bounding_box()
   assert button['y']>=dialog['y'] and button['y']+button['height']<=dialog['y']+dialog['height']+1
   assert page.locator('#phone').evaluate('(el)=>el.scrollWidth<=el.clientWidth')
   if scene=='keyboard':
    key=page.locator('.keyboard').bounding_box()
    assert dialog['y']+dialog['height']<=key['y']
 # Check navigation, opening/editing, draft and sequential input.
 page.evaluate("showScene('analytics')")
 page.locator('#phone').get_by_role('button',name='Операции',exact=True).click()
 page.locator('[data-edit]').first.click()
 assert page.locator('.dialog-head h2').inner_text()=='Изменить операцию'
 page.locator('[data-action=close]').click()
 page.locator('.add').click()
 page.locator('#amount').fill('1250')
 page.locator('#place').fill('Озон')
 page.locator('[data-action=close]').click()
 page.locator('.add').click()
 assert page.locator('#amount').input_value()=='1250'
 assert page.locator('#place').input_value()=='Озон'
 page.locator('[data-action=save]').click()
 assert page.locator('.dialog').count()==1
 assert page.locator('#amount').input_value()==''
 assert page.locator('#place').input_value()==''
 page.evaluate("showScene('transfer')")
 page.locator('#amount').fill('1000')
 page.locator('#target').select_option('Карта')
 page.locator('[data-action=save]').click()
 assert 'разных' in page.locator('.toast').inner_text()
 # Narrow phone: form stays inside the available width, save remains visible.
 page.set_viewport_size({'width':360,'height':850})
 page.evaluate("showScene('keyboard')")
 assert page.locator('.phone').evaluate('(el)=>el.scrollWidth<=el.clientWidth')
 assert not errors,errors
 page.set_viewport_size({'width':1400,'height':1050})
 def board(file,title,items):
  cards=''
  for scene,label,desc in items:
   img=base64.b64encode((root/f'{scene}.png').read_bytes()).decode()
   cards+=f'<article><h2>{label}</h2><p>{desc}</p><img src="data:image/png;base64,{img}"></article>'
  body=f'<html lang="ru"><meta charset="utf-8"><style>body{{background:#e8e8e8;color:#16171d;font-family:Arial,sans-serif;margin:0;padding:32px}}h1{{font-size:28px;margin:0 0 8px}}.sub{{font-size:13px;color:#62656e;margin:0 0 28px}}.grid{{display:flex;gap:24px}}article{{width:414px}}h2{{font-size:19px;margin:0 0 8px}}p{{font-size:12px;color:#62656e;margin:0 0 16px;min-height:34px;line-height:1.5}}img{{width:390px;display:block;border-radius:28px}}</style><h1>{title}</h1><div class="sub">Simple Tasks · черновики для согласования · суммы условные</div><div class="grid">{cards}</div></html>'
  page.set_content(body)
  page.screenshot(path=str(root/file),full_page=True)
 board('main-screens.png','Финансы · основные экраны',[
  ('analytics','01 · Аналитика','Расходы и сравнение по категориям.'),
  ('operations','02 · Операции','Компактная история с группировкой по датам.'),
  ('accounts','03 · Счета','Общая сумма и остатки по счетам.')])
 board('operation-forms.png','Финансы · центральные окна',[
  ('expense','04 · Новый расход','Сумма, счёт, категория и необязательные поля.'),
  ('keyboard','05 · Ввод суммы','Условная клавиатура; поля окна прокручиваются.'),
  ('transfer','06 · Перевод','Два счёта, дата и комментарий.')])
 print('Rendered 9 screens and 2 boards; navigation, draft, save, transfer validation and narrow layout checks passed; no JS errors.')
 b.close()
