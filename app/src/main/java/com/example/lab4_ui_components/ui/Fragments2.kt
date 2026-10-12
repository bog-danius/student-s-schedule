package com.example.lab4_ui_components.ui

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.lab4_ui_components.App
import com.example.lab4_ui_components.MainActivity
import com.example.lab4_ui_components.R
import com.example.lab4_ui_components.data.Task
import com.example.lab4_ui_components.databinding.DialogProfileEditBinding
import com.example.lab4_ui_components.databinding.FragmentNewsBinding
import com.example.lab4_ui_components.databinding.FragmentNotificationsBinding
import com.example.lab4_ui_components.databinding.FragmentProfileBinding
import com.example.lab4_ui_components.databinding.FragmentSettingsBinding
import com.example.lab4_ui_components.databinding.FragmentSyncBinding
import com.example.lab4_ui_components.databinding.FragmentTaskEditBinding
import com.example.lab4_ui_components.util.Fmt
import com.example.lab4_ui_components.util.ThemeHelper
import com.example.lab4_ui_components.util.toLocalDateTime
import com.example.lab4_ui_components.util.toMillis
import com.example.lab4_ui_components.work.Notifier
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class TaskEditFragment : Fragment(R.layout.fragment_task_edit) {
    private var date: LocalDate = LocalDate.now()
    private var time: LocalTime = LocalTime.of(18, 0)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("d", date.toEpochDay())
        outState.putInt("t", time.hour * 60 + time.minute)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentTaskEditBinding.bind(view)
        val nav = findNavController()
        val repo = (requireActivity().application as App).repository
        val args = requireArguments()
        val taskId = args.getLong("taskId")
        var existing: Task? = null

        fun renderDateTime() {
            b.etDate.setText(Fmt.fullDate(date))
            b.etTime.setText(Fmt.time(time.hour * 60 + time.minute))
        }

        b.btnBack.setOnClickListener { nav.popBackStack() }
        b.tvBarTitle.text = if (taskId == 0L) "Новая задача" else "Редактирование"
        b.btnDelete.isVisible = taskId != 0L
        b.btnSave.isEnabled = taskId == 0L

        if (savedInstanceState != null) {
            date = LocalDate.ofEpochDay(savedInstanceState.getLong("d"))
            val m = savedInstanceState.getInt("t")
            time = LocalTime.of(m / 60, m % 60)
        } else if (taskId == 0L) {
            val prefillAt = args.getLong("prefillAt", -1L)
            if (prefillAt > 0) {
                val dt = prefillAt.toLocalDateTime()
                date = dt.toLocalDate(); time = dt.toLocalTime().withSecond(0).withNano(0)
            } else {
                date = LocalDate.now()
            }
            args.getString("prefillTitle")?.let { b.etTitle.setText(it) }
            args.getString("prefillDesc")?.let { b.etDescription.setText(it) }
        }
        renderDateTime()

        if (taskId != 0L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val t = repo.task(taskId)
                if (t == null) { nav.popBackStack(); return@launch }
                existing = t
                if (savedInstanceState == null) {
                    b.etTitle.setText(t.title)
                    b.etDescription.setText(t.description)
                    b.swRemind.isChecked = t.remind
                    val dt = t.dueAt.toLocalDateTime()
                    date = dt.toLocalDate(); time = dt.toLocalTime()
                    renderDateTime()
                }
                b.btnSave.isEnabled = true
            }
        }

        b.etDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Выберите дату")
                .setSelection(date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build()
            picker.addOnPositiveButtonClickListener { sel ->
                date = Instant.ofEpochMilli(sel).atZone(ZoneOffset.UTC).toLocalDate()
                renderDateTime()
            }
            picker.show(childFragmentManager, "date")
        }

        b.etTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(time.hour).setMinute(time.minute)
                .setTitleText("Выберите время")
                .build()
            picker.addOnPositiveButtonClickListener {
                time = LocalTime.of(picker.hour, picker.minute)
                renderDateTime()
            }
            picker.show(childFragmentManager, "time")
        }

        b.btnSave.setOnClickListener {
            val title = b.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                b.etTitle.error = "Введите название задачи"
                b.etTitle.requestFocus()
                return@setOnClickListener
            }
            val base = existing ?: Task(title = "", description = "", dueAt = 0L)
            val task = base.copy(
                title = title,
                description = b.etDescription.text.toString().trim(),
                dueAt = LocalDateTime.of(date, time).toMillis(),
                remind = b.swRemind.isChecked
            )
            viewLifecycleOwner.lifecycleScope.launch {
                repo.saveTask(task)
                Toast.makeText(requireContext(), "Задача сохранена", Toast.LENGTH_SHORT).show()
                nav.popBackStack()
            }
        }

        b.btnDelete.setOnClickListener {
            val t = existing ?: return@setOnClickListener
            Dialogs.confirmDeleteTask(requireContext()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    repo.deleteTask(t)
                    Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
                    nav.popBackStack()
                }
            }
        }
    }
}

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val vm: ScheduleViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentProfileBinding.bind(view)
        val nav = findNavController()
        val prefs = (requireActivity().application as App).prefs

        fun render() {
            b.tvAvatar.text = prefs.initials()
            b.tvName.text = prefs.profileName
            b.tvRole.text = "Студент • ${prefs.group}"
            b.tvGroup.text = prefs.group
            b.tvCourse.text = prefs.course
            b.tvSpecialty.text = prefs.specialty
        }
        render()

        val version = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
        b.tvVersion.text = version

        vm.todayLessons.observe(viewLifecycleOwner) { b.tvToday.text = it.size.toString() }

        b.btnSettings.setOnClickListener { nav.navigate(R.id.settingsFragment) }
        b.rowSettings.setOnClickListener { nav.navigate(R.id.settingsFragment) }
        b.rowNews.setOnClickListener { nav.navigate(R.id.newsFragment) }
        b.rowAbout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.app_name))
                .setMessage("Версия $version\nРасписание занятий, задачи и напоминания.")
                .setPositiveButton("OK", null)
                .show()
        }
        b.rowEdit.setOnClickListener {
            val d = DialogProfileEditBinding.inflate(layoutInflater)
            d.etName.setText(prefs.profileName)
            d.etGroup.setText(prefs.group)
            d.etCourse.setText(prefs.course)
            d.etSpecialty.setText(prefs.specialty)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Редактировать профиль")
                .setView(d.root)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить") { _, _ ->
                    val name = d.etName.text.toString().trim()
                    if (name.isNotEmpty()) prefs.profileName = name
                    prefs.group = d.etGroup.text.toString().trim()
                    prefs.course = d.etCourse.text.toString().trim()
                    prefs.specialty = d.etSpecialty.text.toString().trim()
                    render()
                }
                .show()
        }
    }
}

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSettingsBinding.bind(view)
        val nav = findNavController()
        val prefs = (requireActivity().application as App).prefs
        b.btnBack.setOnClickListener { nav.popBackStack() }

        b.swAuto.isChecked = prefs.autoTheme
        b.swDark.isChecked = prefs.darkTheme
        b.swDark.isEnabled = !prefs.autoTheme
        b.swLessons.isChecked = prefs.lessonReminders
        b.swTasks.isChecked = prefs.taskReminders
        b.swSound.isChecked = prefs.sound
        b.swVibration.isChecked = prefs.vibration

        b.swAuto.setOnCheckedChangeListener { _, c ->
            prefs.autoTheme = c
            b.swDark.isEnabled = !c
            ThemeHelper.apply(prefs)
        }
        b.swDark.setOnCheckedChangeListener { _, c ->
            prefs.darkTheme = c
            ThemeHelper.apply(prefs)
        }
        b.swLessons.setOnCheckedChangeListener { _, c -> prefs.lessonReminders = c }
        b.swTasks.setOnCheckedChangeListener { _, c -> prefs.taskReminders = c }
        b.swSound.setOnCheckedChangeListener { _, c -> prefs.sound = c }
        b.swVibration.setOnCheckedChangeListener { _, c -> prefs.vibration = c }

        b.tvLastSync.text = if (prefs.lastSync == 0L) "Ещё не выполнялась"
        else "Обновлено: ${Fmt.dateTimeLabel(prefs.lastSync)}"
        b.rowSync.setOnClickListener { nav.navigate(R.id.syncFragment) }

        b.rowTest.setOnClickListener {
            val act = requireActivity() as MainActivity
            if (!act.hasNotificationPermission()) {
                act.requestNotificationPermission()
                Toast.makeText(requireContext(), "Разрешите уведомления для приложения", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val ctx = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                Notifier.post(ctx, 1, "Тестовое напоминание", "Так будут выглядеть напоминания о занятиях и задачах.")
            }
        }
    }
}

class NewsFragment : Fragment(R.layout.fragment_news) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentNewsBinding.bind(view)
        val repo = (requireActivity().application as App).repository
        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        val adapter = NewsAdapter()
        b.rvNews.adapter = adapter
        repo.news.asLiveData().observe(viewLifecycleOwner) { adapter.submitList(it) }
    }
}

class NotificationsFragment : Fragment(R.layout.fragment_notifications) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentNotificationsBinding.bind(view)
        val repo = (requireActivity().application as App).repository
        b.btnBack.setOnClickListener { findNavController().popBackStack() }
        val adapter = NotificationAdapter()
        b.rvHistory.adapter = adapter

        b.btnClear.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { repo.clearNotifications() }
        }

        repo.notifications.asLiveData().observe(viewLifecycleOwner) { list ->
            val latest = list.firstOrNull()
            b.latestCard.isVisible = latest != null
            b.tvEmpty.isVisible = latest == null
            b.btnClear.isVisible = latest != null
            if (latest != null) {
                b.tvLatestTitle.text = latest.title
                b.tvLatestText.text = latest.text
                b.tvLatestTime.text = Fmt.dateTimeLabel(latest.timestamp)
            }
            val rest = list.drop(1)
            b.tvHistory.isVisible = rest.isNotEmpty()
            adapter.submitList(rest)
        }
    }
}

class SyncFragment : Fragment(R.layout.fragment_sync) {
    private var animator: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = FragmentSyncBinding.bind(view)
        val repo = (requireActivity().application as App).repository

        animator = ObjectAnimator.ofFloat(b.skeletons, View.ALPHA, 1f, 0.35f).apply {
            duration = 700
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1500)
            repo.sync()
            Toast.makeText(requireContext(), "Расписание обновлено", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        animator?.cancel()
        animator = null
        super.onDestroyView()
    }
}
