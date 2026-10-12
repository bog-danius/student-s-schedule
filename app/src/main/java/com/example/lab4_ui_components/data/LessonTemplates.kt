package com.example.lab4_ui_components.data

import java.time.DayOfWeek
import java.time.LocalDate

/** Недельный шаблон расписания: из него генерируются конкретные занятия по датам. */
object LessonTemplates {
    private class Subject(
        val title: String, val type: String, val room: String,
        val teacher: String, val description: String
    )

    private val subjects = listOf(
        Subject("Android разработка", "Лабораторная работа", "302", "Иванов И.И.",
            "Разработка интерфейса Android-приложения с использованием LinearLayout, ConstraintLayout, View Binding и обработчиков пользовательских событий."),
        Subject("Базы данных", "Практическое занятие", "215", "Петров П.П.",
            "Проектирование реляционных баз данных, SQL-запросы, нормализация и работа с Room."),
        Subject("Проектирование ПО", "Лекция", "108", "Сидоров А.А.",
            "UML-диаграммы, паттерны проектирования и архитектура мобильных приложений."),
        Subject("Английский язык", "Практическое занятие", "412", "Козлова Е.В.",
            "Профессиональная лексика и чтение технической документации."),
        Subject("Операционные системы", "Лекция", "205", "Миронов С.Д.",
            "Процессы, потоки, планирование и управление памятью."),
        Subject("Физическая культура", "Практическое занятие", "Спортзал", "Орлов В.Н.",
            "Общая физическая подготовка."),
        Subject("Алгоритмы и структуры данных", "Лабораторная работа", "310", "Новикова Т.Л.",
            "Реализация деревьев, графов и алгоритмов поиска.")
    )

    // пары (начало, конец) в минутах
    private val slots = listOf(540 to 630, 645 to 735, 780 to 870, 885 to 975)

    // индексы предметов по порядку пар в течение дня
    private val week: Map<DayOfWeek, List<Int>> = mapOf(
        DayOfWeek.MONDAY to listOf(0, 1, 2, 3),
        DayOfWeek.TUESDAY to listOf(4, 6, 1, 5),
        DayOfWeek.WEDNESDAY to listOf(2, 0, 3),
        DayOfWeek.THURSDAY to listOf(6, 4, 0, 1),
        DayOfWeek.FRIDAY to listOf(1, 2, 6, 3),
        DayOfWeek.SATURDAY to listOf(0, 1, 2, 3)
    )

    fun forDay(date: LocalDate): List<Lesson> {
        val order = week[date.dayOfWeek] ?: return emptyList()
        return order.mapIndexed { i, idx ->
            val s = subjects[idx]
            Lesson(
                title = s.title, type = s.type, room = s.room, teacher = s.teacher,
                description = s.description, date = date.toEpochDay(),
                startMinutes = slots[i].first, endMinutes = slots[i].second
            )
        }
    }
}
