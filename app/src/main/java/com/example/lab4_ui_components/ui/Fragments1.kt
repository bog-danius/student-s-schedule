package com.example.lab4_ui_components.ui

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.lab4_ui_components.App
import com.example.lab4_ui_components.R
import com.example.lab4_ui_components.data.Task
import com.example.lab4_ui_components.databinding.FragmentHomeBinding
import com.example.lab4_ui_components.databinding.FragmentLessonDetailBinding
import com.example.lab4_ui_components.databinding.FragmentScheduleBinding
import com.example.lab4_ui_components.databinding.FragmentTasksBinding
import com.example.lab4_ui_components.util.Fmt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private fun Fragment.openLesson(id: Long) =
    findNavController().navigate(R.id.lessonDetailFragment, bundleOf("lessonId" to id))

class HomeFragment : Fragment(R.layout.fragment_home) {
    private val vm: ScheduleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentHomeBinding.bind(view)
        val nav = findNavController()
        val prefs = (requireActivity().application as App).prefs

        b.tvGreeting.text = "Привет, ${prefs.firstName()} 👋"
        b.btnSettings.setOnClickListener { nav.navigate(R.id.settingsFragment) }

        val dayAdapter = DayAdapter { vm.homeDate.value = it }
        val lessonAdapter = LessonAdapter { openLesson(it.id) }
        b.rvDays.adapter = dayAdapter
        b.rvLessons.adapter = lessonAdapter

        fun renderDays() {
            val today = vm.today.value ?: return
            val sel = vm.homeDate.value ?: return
            dayAdapter.submit((-2L..2L).map { today.plusDays(it) }, sel)
            b.tvSelectedLabel.text = if (sel == today) "Сегодня" else Fmt.dayMonth(sel)
            b.tvListTitle.text = if (sel == today) "Ближайшие занятия" else "Занятия, ${Fmt.dayMonth(sel)}"
            b.tvEmpty.text = if (sel == today) "Сегодня занятий нет 🎉" else "В этот день занятий нет 🎉"
        }

        vm.today.observe(viewLifecycleOwner) { b.tvDate.text = Fmt.weekdayDate(it); renderDays() }
        vm.homeDate.observe(viewLifecycleOwner) { renderDays() }
        vm.homeLessons.observe(viewLifecycleOwner) {
            lessonAdapter.submitList(it)
            b.tvEmpty.isVisible = it.isEmpty()
        }
        vm.todayLessons.observe(viewLifecycleOwner) { l ->
            b.tvHeroCount.text = if (l.isEmpty()) "Занятий нет" else Fmt.lessons(l.size)
            b.tvHeroSub.text = if (l.isEmpty()) "Сегодня можно отдохнуть"
            else "Первое занятие в ${Fmt.time(l.first().startMinutes)}"
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshToday()
    }
}

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private val vm: ScheduleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentScheduleBinding.bind(view)
        val nav = findNavController()

        val dayAdapter = DayAdapter { vm.scheduleDate.value = it }
        val lessonAdapter = LessonAdapter { openLesson(it.id) }
        b.rvDays.adapter = dayAdapter
        b.rvLessons.adapter = lessonAdapter

        b.btnMenu.setOnClickListener {
            PopupMenu(requireContext(), b.btnMenu).apply {
                menu.add(0, 1, 0, "🔔 Уведомления")
                menu.add(0, 2, 1, "↻ Синхронизация")
                setOnMenuItemClickListener {
                    nav.navigate(if (it.itemId == 1) R.id.notificationsFragment else R.id.syncFragment)
                    true
                }
                show()
            }
        }

        var scrolled = false
        fun renderDays() {
            val today = vm.today.value ?: return
            val sel = vm.scheduleDate.value ?: return
            val days = (-7L..21L).map { today.plusDays(it) }
            dayAdapter.submit(days, sel)
            b.tvDay.text = Fmt.dayMonth(sel)
            if (!scrolled) {
                scrolled = true
                val idx = days.indexOf(sel).coerceAtLeast(0)
                b.rvDays.post {
                    (b.rvDays.layoutManager as androidx.recyclerview.widget.LinearLayoutManager)
                        .scrollToPositionWithOffset((idx - 2).coerceAtLeast(0), 0)
                }
            }
        }

        vm.today.observe(viewLifecycleOwner) { renderDays() }
        vm.scheduleDate.observe(viewLifecycleOwner) { renderDays() }
        vm.scheduleLessons.observe(viewLifecycleOwner) {
            lessonAdapter.submitList(it)
            b.tvCount.text = Fmt.lessons(it.size)
            b.tvEmpty.isVisible = it.isEmpty()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshToday()
    }
}

class LessonDetailFragment : Fragment(R.layout.fragment_lesson_detail) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentLessonDetailBinding.bind(view)
        val nav = findNavController()
        val repo = (requireActivity().application as App).repository
        val id = requireArguments().getLong("lessonId")

        b.btnBack.setOnClickListener { nav.popBackStack() }
        b.btnAddTask.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val l = repo.lesson(id)
            if (l == null) {
                nav.popBackStack()
                return@launch
            }
            val date = LocalDate.ofEpochDay(l.date)
            b.tvType.text = l.type
            b.tvTitle.text = l.title
            b.tvDate.text = Fmt.dayMonth(date)
            b.tvTime.text = "${Fmt.time(l.startMinutes)}–${Fmt.time(l.endMinutes)}"
            b.tvRoom.text = l.room
            b.tvTeacher.text = l.teacher
            b.tvDescription.text = l.description
            b.btnAddTask.isEnabled = true
            b.btnAddTask.setOnClickListener {
                val at = LocalDateTime.of(date, LocalTime.of(l.startMinutes / 60, l.startMinutes % 60))
                val millis = at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                nav.navigate(
                    R.id.taskEditFragment,
                    bundleOf(
                        "prefillTitle" to "${l.title}: ${l.type.lowercase()}",
                        "prefillDesc" to "Подготовиться к занятию, каб. ${l.room}.",
                        "prefillAt" to millis
                    )
                )
            }
        }
    }
}

class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private val vm: TasksViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentTasksBinding.bind(view)
        val nav = findNavController()

        fun add() = nav.navigate(R.id.taskEditFragment)
        b.btnAdd.setOnClickListener { add() }
        b.fab.setOnClickListener { add() }
        b.btnEmptyAdd.setOnClickListener { add() }

        val adapter = TaskAdapter(
            onToggle = { vm.toggle(it) },
            onEdit = { nav.navigate(R.id.taskEditFragment, bundleOf("taskId" to it.id)) },
            onDelete = { task: Task -> Dialogs.confirmDeleteTask(requireContext()) { vm.delete(task) } }
        )
        b.rvTasks.adapter = adapter
        vm.rows.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            b.emptyState.isVisible = it.isEmpty()
            b.rvTasks.isVisible = it.isNotEmpty()
        }
    }
}
