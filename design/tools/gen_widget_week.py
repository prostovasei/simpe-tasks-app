"""Генератор разметки вида «Неделя» в виджете и таблицы id RemoteViews.
#313 (вариант PM 5 «Всё листается»): у каждого дня свой список задач под неподвижной строкой даты, мини-календарь — лента недель
с названиями месяцев. Разметка корня статическая (без addView): списки живут, пока живёт виджет, и помнят, куда их долистали.
#263: в строке задачи эмодзи списка → название → начало справа; вся ячейка открывает день.
Запуск из корня: python3 design/tools/gen_widget_week.py
"""
import os

DAYS = 7
RES = 'app/src/main/res/layout'
KT = 'app/src/main/java/com/prostovasei/simpletasks/widget/WidgetWeekIds.kt'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:tools="http://schemas.android.com/tools"'
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n'
LIST = '''android:divider="@null"
            android:dividerHeight="0dp"
            android:fadingEdgeLength="@dimen/widget_week_fade"
            android:listSelector="@android:color/transparent"
            android:overScrollMode="never"
            android:requiresFadingEdge="vertical"
            android:scrollbars="none"'''


def day(n):
    return f'''
        <LinearLayout
            android:id="@+id/w{n}_root"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/widget_week_cell"
            android:orientation="vertical"
            android:paddingStart="@dimen/widget_week_edge"
            android:paddingTop="@dimen/widget_week_cell_top"
            android:paddingEnd="@dimen/widget_space_xxs">

            <LinearLayout
                android:id="@+id/w{n}_head"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:minHeight="@dimen/widget_week_header"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/w{n}_wd"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:includeFontPadding="false"
                    android:maxLines="1"
                    android:textColor="@color/widget_text_primary"
                    android:textSize="9sp" />

                <TextView
                    android:id="@+id/w{n}_num"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:layout_marginStart="@dimen/widget_space_xxs"
                    android:ellipsize="end"
                    android:includeFontPadding="false"
                    android:maxLines="1"
                    android:textColor="@color/widget_text_secondary"
                    android:textSize="9sp" />
            </LinearLayout>

            <ListView
                android:id="@+id/w{n}_list"
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1"
            {LIST} />
        </LinearLayout>
'''


def calendar():
    return f'''
        <LinearLayout
            android:id="@+id/wc_root"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="@drawable/widget_week_cell"
            android:orientation="vertical"
            android:paddingStart="@dimen/widget_space_xxs"
            android:paddingEnd="@dimen/widget_space_xxs">

            <ListView
                android:id="@+id/wc_list"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
            {LIST} />
        </LinearLayout>
'''


def root():
    rows = ''
    for r in range(4):
        left = day(r * 2 + 1)
        right = day(r * 2 + 2) if r < 3 else calendar()
        rows += f'''
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:orientation="horizontal">
{left}{right}        </LinearLayout>
'''
    return HEAD + f'''<!--
  Сгенерировано design/tools/gen_widget_week.py (#313, вариант PM 5 «Всё листается»): корень виджета на вкладке «Неделя».
  Четыре строки делят настоящую высоту виджета поровну (weight, 0.46.0: рабочий стол сообщает размер больше настоящего);
  у каждого дня под строкой даты — свой список задач, в последней ячейке — лента недель календаря. Разметка статическая:
  списки не пересоздаются при обновлении и помнят прокрутку. Снизу — место под панель (widget_overlay от Glance поверх).
  Не править руками.
-->
<FrameLayout {NS}
    tools:ignore="TooManyViews,NestedWeights,SmallSp"
    android:id="@+id/widget_frame"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/widget_root_bg"
    android:clipToOutline="true"
    tools:targetApi="31">

    <LinearLayout
        android:id="@+id/week_rows"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginStart="@dimen/widget_list_edge"
        android:layout_marginTop="@dimen/widget_week_top"
        android:layout_marginBottom="@dimen/widget_bar"
        android:orientation="vertical">
{rows}    </LinearLayout>

    <FrameLayout
        android:id="@+id/widget_overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</FrameLayout>
'''


def task():
    return HEAD + f'''<!-- Сгенерировано design/tools/gen_widget_week.py (#313): строка задачи в списке дня — эмодзи, название, начало справа. Не править руками. -->
<FrameLayout {NS}
    tools:ignore="UselessParent"
    android:id="@+id/wi_row"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="@dimen/widget_week_item"
    android:paddingTop="@dimen/widget_week_task_gap">

    <LinearLayout
        android:id="@+id/wi_plaque"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="@dimen/widget_week_task"
        android:layout_gravity="center_vertical"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/wi_emoji"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="@dimen/widget_week_content_gap"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textSize="10sp" />

        <TextView
            android:id="@+id/wi_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="@color/widget_text_primary"
            android:textSize="10sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/wi_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/widget_week_content_gap"
            android:fontFeatureSettings="tnum"
            android:gravity="end"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/widget_text_primary"
            android:textSize="10sp"
            android:textStyle="bold" />
    </LinearLayout>
</FrameLayout>
'''


def filler():
    return HEAD + f'''<!-- Сгенерировано design/tools/gen_widget_week.py (#313): пустое место под задачами дня — открывает день; высота (наименьшая) задаётся кодом: высоту из разметки список у строки не учитывает. Не править руками. -->
<FrameLayout {NS}
    android:id="@+id/wi_filler"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="@dimen/widget_week_item" />
'''


def cal_week():
    days = ''.join(f'''
    <TextView
        android:id="@+id/wcw_d{d}"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:gravity="center"
        android:includeFontPadding="false"
        android:maxLines="1"
        android:textColor="@color/widget_text_primary"
        android:textSize="11sp" />
''' for d in range(1, 8))
    return HEAD + f'''<!-- Сгенерировано design/tools/gen_widget_week.py (#313): неделя в ленте календаря — семь чисел. Не править руками. -->
<LinearLayout {NS}
    tools:ignore="DisableBaselineAlignment,SmallSp"
    android:id="@+id/wcw_row"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="@dimen/widget_week_cal_row"
    android:gravity="center_vertical"
    android:orientation="horizontal">
{days}</LinearLayout>
'''


def cal_month():
    return HEAD + f'''<!-- Сгенерировано design/tools/gen_widget_week.py (#313): название месяца в ленте календаря. Не править руками. -->
<TextView {NS}
    tools:ignore="SmallSp"
    android:id="@+id/wcm_title"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="@dimen/widget_week_cal_month"
    android:gravity="bottom|start"
    android:includeFontPadding="false"
    android:maxLines="1"
    android:paddingStart="@dimen/widget_week_edge"
    android:textColor="@color/widget_text_secondary"
    android:textSize="9sp"
    android:textStyle="bold" />
'''


def ids_kt():
    days = ',\n'.join(f'        DayIds(R.id.w{n}_root, R.id.w{n}_head, R.id.w{n}_wd, R.id.w{n}_num, R.id.w{n}_list)' for n in range(1, DAYS + 1))
    cal = ', '.join(f'R.id.wcw_d{d}' for d in range(1, 8))
    return f'''package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (#313): id элементов вида «Неделя» в виджете. Не править руками.

/** Ячейка дня: корень, строка даты (день недели и число) и список задач. */
class DayIds(val root: Int, val head: Int, val weekday: Int, val number: Int, val list: Int)

object WeekIds {{
    /** Дни недели по порядку: пн … вс. */
    val DAYS = listOf(
{days},
    )

    /** Лента недель мини-календаря. */
    val CALENDAR = R.id.wc_list

    /** Строка недели в ленте календаря: её семь чисел. */
    val CAL_DAYS = listOf({cal})
}}
'''


os.makedirs(RES, exist_ok=True)
for old in ['widget_week_cell_l', 'widget_week_cell_r', 'widget_week_calendar', 'widget_row_week', 'widget_row_week_last']:
    path = f'{RES}/{old}.xml'
    if os.path.exists(path):
        os.remove(path)
open(f'{RES}/widget_root_week.xml', 'w', encoding='utf-8').write(root())
open(f'{RES}/widget_week_task.xml', 'w', encoding='utf-8').write(task())
open(f'{RES}/widget_week_filler.xml', 'w', encoding='utf-8').write(filler())
open(f'{RES}/widget_week_cal_week.xml', 'w', encoding='utf-8').write(cal_week())
open(f'{RES}/widget_week_cal_month.xml', 'w', encoding='utf-8').write(cal_month())
open(KT, 'w', encoding='utf-8').write(ids_kt())
print('готово')
