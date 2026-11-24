rows_today=[("03:00",None,"Футбол","🗂️",True,True,False),("10:00",None,"ChatGPT","💲",True,False,True),("14:00","16:00","Оценка Займы","💼",True,False,False,),("16:00",None,"Пройти юр.тест","💼",True,False,False),("16:00","17:30","Оценка","💼",False,False,True),("19:00",None,"Икра","🗂️",True,False,False)]
pri={"Оценка Займы","Пройти юр.тест"}
def row(t,t2,title,emo,bell,done,rep):
    cls="row"+(" done" if done else "")+(" pri" if title in pri else "")
    ring='<span class="chk %s">%s</span>'%("rep" if rep else "", "✓" if done else ("↻" if rep else ""))
    tm=t+("<br>"+t2 if t2 else "")
    return f'<div class="{cls}"><span class="tm">{tm}</span>{ring}<span class="tt">{title}</span><span class="ic">{"🔔" if bell else ""}</span><span class="em">{emo}</span></div>'
def day(d,lbl,prog,cnt,rows,late=False):
    bar=f'<span class="bar"><i style="width:{prog}%"></i></span>' if prog is not None else '<span class="sp"></span>'
    return f'<div class="day{" late" if late else ""}"><b>{d}</b><span class="lb">{lbl}</span>{bar}<span class="cnt">{cnt}</span></div>'+"".join(row(*r) for r in rows)
body=day("ВТ, 24 ноя","Вчера",None,"1",[("10:00",None,"Claude","💲",True,False,True)],True)+day("СР, 25 ноя","Сегодня",17,"1 из 6",rows_today)+day("ЧТ, 26 ноя","Завтра",None,"5",[("03:00",None,"Футбол","🗂️",True,False,False),("11:00",None,"Дейлик","💼",False,False,True),("14:30",None,"Девопсы","💼",False,False,True)])
cols="".join(f'<section><h2>{g} dp</h2><p>{d}</p><div class="phone" style="--g:{g}px">{body}</div></section>' for g,d in [(6,"Чуть свободнее, почти как сейчас"),(8,"Рекомендую: заметно легче, строка почти не сужается"),(12,"Много воздуха, название короче на ~5 символов")])
html=open("tpl.html").read().replace("%COLS%",cols)
open("prototype.html","w").write(html)
