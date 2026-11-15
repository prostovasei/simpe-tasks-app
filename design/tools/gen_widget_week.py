"""Генератор разметки вида «Неделя» в виджете (итерация 45; 48 — мельче дата и название, время слева от кружка (#249), эмодзи справа, макет week_tune.py; 50 — зона нажатия кружка ~40 dp поверх начала строки, макет iteration50.py; 0.46.0 — компактные строки, календарь без месяца и стрелок, строки недели, строки задач
и недели календаря делят высоту поровну — weight, а не размер от рабочего стола): ячейки дней, мини-календарь и строки ленты + таблица id для Kotlin.

Ячейка и календарь состоят из десятков элементов с уникальными id (в RemoteViews id внутри одной разметки не должны повторяться, а левая и правая
ячейка — разные поддеревья), поэтому XML и таблица id пишутся скриптом. Запуск из корня репозитория: python3 design/tools/gen_widget_week.py
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
                android:id="@+id/{p}_s{i}_time"
                android:layout_width="wrap_content"
                android:layout_marginEnd="@dimen/widget_week_content_gap"
                android:fontFeatureSettings="tnum"
                android:layout_height="wrap_content"
                android:gravity="start"
                android:includeFontPadding="false"
                android:maxLines="1"
                android:textColor="@color/widget_text_secondary"
                android:textSize="10sp" />
            <FrameLayout
                android:id="@+id/{p}_s{i}_check"
                android:layout_width="@dimen/widget_week_check"
                android:layout_marginEnd="@dimen/widget_week_content_gap"
                android:layout_height="match_parent">

                <ImageView
                    android:id="@+id/{p}_s{i}_node"
                    android:layout_width="@dimen/widget_week_check"
                    android:layout_height="@dimen/widget_week_check"
                    android:layout_gravity="center"
                    android:contentDescription="@string/widget_complete"
                    android:src="@drawable/ic_widget_node_ring" />

                <ImageView
                    android:id="@+id/{p}_s{i}_tick"
                    android:layout_width="@dimen/widget_week_tick"
                    android:layout_height="@dimen/widget_week_tick"
                    android:layout_gravity="center"
                    android:contentDescription="@null"
                    android:src="@drawable/ic_node_check"
                    android:tint="@color/widget_task_surface"
                    android:visibility="gone" />
            </FrameLayout>

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
                android:id="@+id/{p}_s{i}_emoji"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="@dimen/widget_space_xxs"
                android:includeFontPadding="false"
                android:maxLines="1"
                android:textSize="10sp" />

        </LinearLayout>

            <!-- Итерация 50: невидимая зона «выполнить» поверх кружка и начала названия — кружок и название на месте. -->
            <FrameLayout
                android:id="@+id/{p}_s{i}_hit"
                android:layout_width="@dimen/widget_week_hit"
                android:layout_height="match_parent" />
        </FrameLayout>
'''
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано design/tools/gen_widget_week.py (итерация 45; 48 — строка даты по высоте текста, время слева от кружка (#249), эмодзи справа): ячейка дня вида «Неделя» в виджете. Не править руками. -->
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
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="@dimen/widget_space_xxs"
            android:includeFontPadding="false"
            android:maxLines="1"
            android:textColor="@color/widget_text_secondary"
            android:textSize="9sp" />

        <FrameLayout
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1" />

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
                    android:layout_height="@dimen/widget_week_cal_day"
                    android:layout_gravity="center"
                    android:gravity="center"
                    android:textColor="@color/widget_text_primary"
                    android:textSize="11sp"
                    android:textStyle="bold" />
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
    weekdays = ''.join(f'''
            <TextView
                android:id="@+id/wc_wd{d}"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:gravity="center"
                android:maxLines="1"
                android:textColor="@color/widget_inactive"
                android:textSize="9sp" />
''' for d in range(1, 8))
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Сгенерировано design/tools/gen_widget_week.py (итерация 45; 48 — даты 11): мини-календарь вида «Неделя» в виджете. Не править руками. -->
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

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="@dimen/widget_week_cal_weekdays"
        android:orientation="horizontal">
{weekdays}    </LinearLayout>
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
        slots = ',\n'.join(f'        SlotIds(R.id.{p}_s{i}, R.id.{p}_s{i}_check, R.id.{p}_s{i}_node, R.id.{p}_s{i}_tick, R.id.{p}_s{i}_title, R.id.{p}_s{i}_emoji, R.id.{p}_s{i}_time, R.id.{p}_s{i}_hit)' for i in range(1, SLOTS + 1))
        return f'''    val {name} = CellIds(
        head = R.id.{p}_head, weekday = R.id.{p}_wd, number = R.id.{p}_num, more = R.id.{p}_more,
        slots = listOf(
{slots},
        ),
    )
'''.replace('\n        SlotIds', '\n            SlotIds')
    cal_rows = ',\n'.join(f'        CalRow(R.id.wc_r{r}, listOf({", ".join(f"R.id.wc_r{r}_d{d}" for d in range(1, 8))}))' for r in range(1, 7))
    return f'''package com.prostovasei.simpletasks.widget

import com.prostovasei.simpletasks.R

// Сгенерировано design/tools/gen_widget_week.py (итерация 45): id элементов ячеек и мини-календаря вида «Неделя» в виджете. Не править руками.

/**
 * Элементы одной плашки задачи в ячейке: сама плашка, место кружка, кружок, галочка, название, эмодзи списка (итерация 48), время
 * и зона нажатия «выполнить» ~40 dp поверх кружка и начала названия (итерация 50).
 */
class SlotIds(val plaque: Int, val check: Int, val node: Int, val tick: Int, val title: Int, val emoji: Int, val time: Int, val hit: Int)

/** Элементы ячейки дня: строка даты (день недели, число, «+N») и плашки задач. */
class CellIds(val head: Int, val weekday: Int, val number: Int, val more: Int, val slots: List<SlotIds>)

/** Строка недели мини-календаря и её семь дней. */
class CalRow(val row: Int, val days: List<Int>)

object WeekIds {{
    /** Плашек в ячейке не больше. */
    const val SLOTS = {SLOTS}

{cell_ids('wl', 'LEFT')}
{cell_ids('wr', 'RIGHT')}
    val CAL_WEEKDAYS = listOf({", ".join(f"R.id.wc_wd{d}" for d in range(1, 8))})
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
