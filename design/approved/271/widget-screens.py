"""Виджет финансов #271. PNG из HTML-макета, без запуска Android."""

import os
import re
import shutil
import xml.etree.ElementTree as ET
from pathlib import Path

from playwright.sync_api import sync_playwright

HERE = Path(__file__).resolve().parent
DRAWABLE = HERE.parents[2] / "app/src/main/res/drawable"
ANDROID = "{http://schemas.android.com/apk/res/android}"
APP = (HERE / "finance-app.html").read_text()


def icon(name):
    vector = ET.parse(DRAWABLE / f"ic_{name}.xml").getroot()
    width = vector.get(ANDROID + "viewportWidth")
    height = vector.get(ANDROID + "viewportHeight")
    paths = []
    for path in vector:
        fill = path.get(ANDROID + "fillColor")
        attributes = {
            "d": path.get(ANDROID + "pathData"),
            "fill": "currentColor" if fill and "transparent" not in fill else "none",
        }
        for source, target in [("strokeWidth", "stroke-width"), ("strokeLineCap", "stroke-linecap"),
                               ("strokeLineJoin", "stroke-linejoin")]:
            if value := path.get(ANDROID + source):
                attributes[target] = value
        if path.get(ANDROID + "strokeColor"):
            attributes["stroke"] = "currentColor"
        paths.append("<path " + " ".join(f'{key}="{value}"' for key, value in attributes.items()) + "/>")
    return f'<svg viewBox="0 0 {width} {height}" aria-hidden="true">{"".join(paths)}</svg>'


chart = re.search(r'<figure class="timeline".*?</figure>', APP, re.S)[0]
categories, places = re.findall(r'<figure class="share-block">.*?</figure>', APP, re.S)
balance_title = re.search(r'<div id="simple-tasks-accounts">(.*?)</div>', APP, re.S)[1]
balances = re.search(r'(<div class="account-balances">.*?)</section>', APP, re.S)[1]
accounts = f'<section aria-label="Счета"><div class="balance-total">{balance_title}</div>{balances}</section>'
summary = re.search(r'<div class="spending-summary".*?\n        </div>', APP, re.S)[0]
for lucide, drawable in [("receipt-text", "finance_receipt"), ("gauge", "finance_limit"),
                         ("calendar-days", "finance_forecast")]:
    summary = summary.replace(f'<i data-lucide="{lucide}"></i>', icon(drawable))

footer = f'''<footer class="widget-bar">
  <span class="widget-add" aria-label="Добавить">{icon("widget_add")}</span>
  <span class="widget-tabs"><span aria-label="Задачи">{icon("module_tasks")}</span>
    <span aria-label="Финансы" class="selected">{icon("module_finance")}</span></span>
  <span class="widget-month">ноя 26</span>
</footer>'''


def sheet(label, compact=False, position="top"):
    compact_class = " compact" if compact else ""
    body = chart + summary if compact else summary + chart
    body += f'<div class="spending-shares">{categories}{places}</div>' + accounts
    return f'''<figure class="sample">
      <figcaption>{label}</figcaption>
      <div class="widget{compact_class}" data-scroll="{position}">
        <div class="finance-screen">
          <div class="widget-body finance-content">{body}</div>
          {footer}
        </div>
      </div>
    </figure>'''


CSS = re.search(r"<style>(.*?)</style>", APP, re.S)[1]
WIDGET_CSS = """
html { background: #edf0f5; }
body { margin: 0; }
#simple-tasks-spending-chips.board {
  display: grid; grid-template-columns: 320px 376px; gap: 24px;
  width: max-content; margin: 0; padding: 20px; align-items: start;
}
#simple-tasks-spending-chips .sample > figcaption,
#simple-tasks-spending-chips .compact-title { font-size: 13px; font-weight: 700; line-height: 18px; margin: 0 0 9px; }
#simple-tasks-spending-chips .compact-gallery { display: grid; grid-template-columns: repeat(2, 180px); gap: 15px 16px; }
#simple-tasks-spending-chips .compact-gallery figcaption { color: var(--muted-ink); font-weight: 500; font-size: 11px; margin-bottom: 5px; }
#simple-tasks-spending-chips .finance-screen {
  width: 320px; height: 416px; display: grid; grid-template-rows: minmax(0, 1fr) 40px;
  border-radius: 18px; box-shadow: 0 3px 12px #16171d14;
}
#simple-tasks-spending-chips .compact .finance-screen { width: 180px; height: 110px; }
#simple-tasks-spending-chips .widget-body {
  overflow-y: auto; overflow-x: hidden; scrollbar-width: none;
  padding: 6px 6px 9px; display: grid; grid-auto-rows: max-content; gap: 12px;
}
#simple-tasks-spending-chips .compact .widget-body { gap: 8px; padding-bottom: 2px; }
#simple-tasks-spending-chips .spending-summary { margin: 0; font-size: 15px; }
#simple-tasks-spending-chips .spending-summary .app-icon { width: 14px; height: 14px; }
#simple-tasks-spending-chips .compact .spending-summary { font-size: 11px; }
#simple-tasks-spending-chips .compact .spending-summary .app-icon { width: 11px; height: 11px; flex-basis: 11px; }
#simple-tasks-spending-chips .timeline-plot { height: 106px; }
#simple-tasks-spending-chips .compact .timeline { margin: 0; }
#simple-tasks-spending-chips .compact .timeline-plot { height: 47px; }
#simple-tasks-spending-chips .compact .axis-amount,
#simple-tasks-spending-chips .compact .timeline-dates { font-size: 8px; }
#simple-tasks-spending-chips .spending-shares { margin-top: 0; gap: 12px; }
#simple-tasks-spending-chips .share-line { margin-top: 17px; height: 6px; }
#simple-tasks-spending-chips .share-chip { padding: 4px 5px; border-radius: 999px; }
#simple-tasks-spending-chips .compact .share-chip { margin-inline: 1px; padding: 3px; gap: 1px; }
#simple-tasks-spending-chips .compact .share-value { font-size: 9px; }
#simple-tasks-spending-chips .compact .share-line { height: 5px; }
#simple-tasks-spending-chips .compact .chip-name,
#simple-tasks-spending-chips .compact .chip-amount { font-size: 9px; }
#simple-tasks-spending-chips .balance-total { font-size: 14px; }
#simple-tasks-spending-chips .account-balances { margin-top: 5px; gap: 4px; }
#simple-tasks-spending-chips .account-balances > div { gap: 3px; padding: 4px 6px; }
#simple-tasks-spending-chips .account-balances strong { font-size: 11px; }
#simple-tasks-spending-chips .compact .account-balances { grid-template-columns: repeat(3, minmax(0, 1fr)); }
#simple-tasks-spending-chips .compact .account-balances > div { padding-block: 3px; }
#simple-tasks-spending-chips .compact .balance-total { font-size: 11px; }
#simple-tasks-spending-chips .widget-bar {
  display: grid; grid-template-columns: 64px minmax(0, 1fr) 64px;
  align-items: center; padding: 0 6px; color: var(--ink); background: var(--paper);
}
#simple-tasks-spending-chips .compact .widget-bar { grid-template-columns: 40px minmax(0, 1fr) 40px; }
#simple-tasks-spending-chips .widget-bar svg { width: 19px; height: 19px; }
#simple-tasks-spending-chips .widget-add {
  display: grid; place-items: center; height: 32px; border-radius: 12px; background: var(--ink); color: var(--paper);
}
#simple-tasks-spending-chips .widget-tabs { display: flex; justify-content: center; color: #a4a6ac; }
#simple-tasks-spending-chips .widget-tabs > span { display: grid; place-items: center; width: 44px; height: 32px; }
#simple-tasks-spending-chips .widget-tabs .selected { color: var(--ink); }
#simple-tasks-spending-chips .widget-month { text-align: center; color: var(--muted-ink); font-size: 11px; }
"""

html = f'''<!doctype html>
<html lang="ru"><meta charset="utf-8"><title>Финансы · виджет #271</title>
<style>{CSS}{WIDGET_CSS}</style>
<main class="board" id="simple-tasks-spending-chips">
  {sheet("Широкий виджет · 320×416 dp")}
  <section>
    <h2 class="compact-title">Виджет 180×110 dp · прокрутка сверху вниз</h2>
    <div class="compact-gallery">
      {sheet("1. График", compact=True)}
      {sheet("2. Доли категорий", compact=True, position="categories")}
      {sheet("3. Доли мест", compact=True, position="places")}
      {sheet("4. Все счета", compact=True, position="accounts")}
    </div>
  </section>
</main>'''


def render():
    executable = os.environ.get("CHROMIUM_PATH") or shutil.which("chromium")
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(
            executable_path=executable, args=["--no-sandbox"]
        )
        page = browser.new_page(viewport={"width": 760, "height": 510}, device_scale_factor=2)
        page.set_content(html)
        for position, target in [("categories", ".share-block"), ("places", ".share-block + .share-block"),
                                 ("accounts", 'section[aria-label="Счета"]')]:
            widget = page.locator(f'[data-scroll="{position}"] .widget-body')
            widget.evaluate(
                """(body, selector) => {
                    const target = body.querySelector(selector);
                    body.scrollTop += target.getBoundingClientRect().top - body.getBoundingClientRect().top - 6;
                }""",
                target,
            )
        page.locator('[data-scroll="accounts"] .widget-body').evaluate("(body) => body.scrollTop = body.scrollHeight")
        assert page.locator('.finance-screen').evaluate_all(
            """(screens) => screens.slice(1).every(screen =>
                screen.offsetWidth === 180 && screen.offsetHeight === 110 &&
                screen.querySelector(".widget-bar").offsetHeight === 40
            )"""
        ), "Ожидались виджеты 180×110 с нижней панелью 40 px"
        assert page.locator('[data-scroll="accounts"] .widget-body').evaluate(
            """(body) => {
                const viewport = body.getBoundingClientRect();
                const total = body.querySelector(".balance-total").getBoundingClientRect();
                const last = body.querySelector(".account-balances > div:last-child").getBoundingClientRect();
                return total.top >= viewport.top && last.bottom <= viewport.bottom;
            }"""
        ), "Баланс и последний счёт должны целиком помещаться в видимую область"
        page.locator(".board").screenshot(path=str(HERE / "widget-finance.png"))
        browser.close()


if __name__ == "__main__":
    render()
