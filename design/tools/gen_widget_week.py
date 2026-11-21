"""Генератор недельных ячеек, календаря и таблицы id RemoteViews.
#263: эмодзи списка → название → начало справа, без узлов выполнения; вся ячейка открывает день.
Строки недели и слоты задач делят высоту поровну, не более шести задач в ячейке.
Запуск из корня: python3 design/tools/gen_widget_week.py
"""
import os

SLOTS = 6  # строк задач в ячейке не больше (решение PM: до 6)
RES = 'app/src/main/res/layout'
KT = 'app/src/main/java/com/prostovasei/simpletasks/widget/WidgetWeekIds.kt'
NS = 'xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:tools="http://schemas.android.com/tools"'


def cell(p):
    slots = ''
    for i in range(1, SLOTS + 1):
        slots += f'''
        <FrameLayout
            android:id="@+id/{p}_s{i}"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:layout_marginTop="@dimen/widget_week_task_gap"
            android:background="@drawable/widget_week_plaque_plain"
            android:visibility="gone">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <TextView
                android:id="@+id/{p}_s{i}_emoji"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginEnd="@dimen/widget_week_content_gap"
                android:includeFontPadding="false"
                android:maxLines="1"
                android:textSize="10sp" />

            <TextView
                android:id="@+id/{p}_s{i}_title"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:ellipsize="end"
                android:maxLines="1"
                android:textColor="@color/widget_text_primary"
                android:textSize="10sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/{p}_s{i}_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/widget_week_content_gap"
                android:fontFeatureSettings="tnum"
                android:textStyle="bold"
                android:gravity="end"
                android:includeFontPadding="false"
                android:maxLines="1"
                android:textColor="@color/widget_text_primary"
                android:textSize="10sp" />
        </LinearLayout>

        </FrameLayout>
'''
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано design/tools/gen_widget_week.py (#263 — эмодзи, название, время справа; вся ячейка открывает день): ячейка дня вида «Неделя» в виджете. Не править руками. -->
<LinearLayout {NS}
    tools:ignore="UselessLeaf"
    android:id="@+id/{p}_root"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:background="@drawable/widget_week_cell"
    android:orientation="vertical"
    android:paddingStart="@dimen/widget_week_edge"
    android:paddingTop="@dimen/widget_week_cell_top"
    android:paddingEnd="@dimen/widget_space_xxs"
    android:paddingBottom="@dimen/widget_week_task_gap">

    <LinearLayout
        android:id="@+id/{p}_head"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:minHeight="@dimen/widget_week_header"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/{p}_wd"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/widget_text_primary"
            android:textSize="9sp" />

        <TextView
            android:id="@+id/{p}_num"
            android:layout_width="0dp"
            android:layout_weight="1"
            android:ellipsize="end"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/widget_space_xxs"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/widget_text_secondary"
            android:textSize="9sp" />


        <TextView
            android:id="@+id/{p}_more"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/widget_text_primary"
            android:textSize="9sp"
            android:textStyle="bold"
            android:visibility="gone" />
    </LinearLayout>
{slots}</LinearLayout>
'''


def calendar():
    rows = ''
    for r in range(1, 7):
        days = ''.join(f'''
            <FrameLayout
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1">

                <TextView
                    android:id="@+id/wc_r{r}_d{d}"
                    android:layout_width="@dimen/widget_week_cal_day"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center"
                    android:gravity="center"
                    android:textColor="@color/widget_text_primary"
                    android:maxLines="1"
                    android:textSize="11sp" />
            </FrameLayout>
''' for d in range(1, 8))
        rows += f'''
        <LinearLayout
            android:id="@+id/wc_r{r}"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:gravity="center_vertical"
            android:orientation="horizontal">
{days}        </LinearLayout>
'''
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано design/tools/gen_widget_week.py (#284 — светлая полоса): мини-календарь вида «Неделя» в виджете. Не править руками. -->
<LinearLayout {NS}
    tools:ignore="DisableBaselineAlignment,TooManyViews,SmallSp"
    android:id="@+id/wc_root"
    android:layout_width="0dp"
    android:layout_height="match_parent"
    android:layout_weight="1"
    android:background="@drawable/widget_week_cell"
    android:orientation="vertical"
    android:paddingStart="@dimen/widget_space_xxs"
    android:paddingTop="@dimen/widget_space_xxs"
    android:paddingEnd="@dimen/widget_space_xxs"
    android:paddingBottom="@dimen/widget_space_xxs">

{rows}</LinearLayout>
'''


def row(name, comment, right):
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано design/tools/gen_widget_week.py (итерация 45; 0.46.0 — доля высоты): {comment}. Строки недели делят высоту виджета поровну (weight). Не править руками. -->
<LinearLayout {NS}
    tools:ignore="UselessParent"
    android:id="@+id/week_row"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1"
    android:orientation="horizontal">

    <include layout="@layout/widget_week_cell_l" />

    <include layout="@layout/{right}" />
</LinearLayout>
'''


def ids_kt():
    def cell_ids(p, name):
        slots = ',\n'.join(f'        SlotIds(R.id.{p}_s{i}, R.id.{p}_s{i}_title, R.id.{p}_s{i}_emoji, R.id.{p}_s{i}_time)' for i in range(1, SLOTS + 1))
        return f'''    val {name} = CellIds(
        root = R.id.{p}_root, head = R.id.{p}_head, weekday = R.id.{p}_wd, number = R.id.{p}_num, more = R.id.{p}_more,
        slots = listOf(
{slots},
        ),
    )
'''.replace('\n        SlotIds', '\n            SlotIds')
    cal_rows = ',\n'.join(f'        CalRow(R.id.wc_r{r}, listOf({", ".join(f"R.id.wc_r{r}_d{d}" for d in range(1, 8))}))' for r in range(1, 7))
    return f'''package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (итерация 45): id элементов ячеек и мини-календаря вида «Неделя» в виджете. Не править руками.

/** Пассивная строка недели: контейнер, название, эмодзи списка и начало справа. */
class SlotIds(val plaque: Int, val title: Int, val emoji: Int, val time: Int)

/** Элементы ячейки дня: общий корень, строка даты (день недели, число, «+N») и плашки задач. */
class CellIds(val root: Int, val head: Int, val weekday: Int, val number: Int, val more: Int, val slots: List<SlotIds>)

/** Строка недели мини-календаря и её семь дней. */
class CalRow(val row: Int, val days: List<Int>)

object WeekIds {{
    /** Плашек в ячейке не больше. */
    const val SLOTS = {SLOTS}

{cell_ids('wl', 'LEFT')}
{cell_ids('wr', 'RIGHT')}
    val CAL_ROWS = listOf(
{cal_rows},
    )
}}
'''


os.makedirs(RES, exist_ok=True)
open(f'{RES}/widget_week_cell_l.xml', 'w', encoding='utf-8').write(cell('wl'))
open(f'{RES}/widget_week_cell_r.xml', 'w', encoding='utf-8').write(cell('wr'))
open(f'{RES}/widget_week_calendar.xml', 'w', encoding='utf-8').write(calendar())
open(f'{RES}/widget_row_week.xml', 'w', encoding='utf-8').write(row('widget_row_week', 'строка недели: два дня', 'widget_week_cell_r'))
open(f'{RES}/widget_row_week_last.xml', 'w', encoding='utf-8').write(row('widget_row_week_last', 'последняя строка недели: воскресенье и мини-календарь', 'widget_week_calendar'))
open(KT, 'w', encoding='utf-8').write(ids_kt())
print('готово')
