package com.example.lab4_ui_components.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4_ui_components.R
import com.example.lab4_ui_components.data.AppNotification
import com.example.lab4_ui_components.data.Lesson
import com.example.lab4_ui_components.data.News
import com.example.lab4_ui_components.databinding.ItemDayBinding
import com.example.lab4_ui_components.databinding.ItemHeaderBinding
import com.example.lab4_ui_components.databinding.ItemLessonBinding
import com.example.lab4_ui_components.databinding.ItemNewsBinding
import com.example.lab4_ui_components.databinding.ItemNotificationBinding
import com.example.lab4_ui_components.databinding.ItemTaskBinding
import com.example.lab4_ui_components.util.Fmt
import java.time.LocalDate

class DayAdapter(private val onClick: (LocalDate) -> Unit) : RecyclerView.Adapter<DayAdapter.VH>() {
    private var days: List<LocalDate> = emptyList()
    private var selected: LocalDate = LocalDate.now()

    class VH(val b: ItemDayBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(days: List<LocalDate>, selected: LocalDate) {
        this.days = days
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = days[position]
        holder.b.root.isActivated = d == selected
        holder.b.tvDow.text = Fmt.dayShort(d)
        holder.b.tvNum.text = d.dayOfMonth.toString()
        holder.b.root.setOnClickListener { onClick(d) }
    }
}

class LessonAdapter(private val onClick: (Lesson) -> Unit) :
    ListAdapter<Lesson, LessonAdapter.VH>(object : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(a: Lesson, b: Lesson) = a.id == b.id
        override fun areContentsTheSame(a: Lesson, b: Lesson) = a == b
    }) {

    class VH(val b: ItemLessonBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val l = getItem(position)
        with(holder.b) {
            tvTime.text = Fmt.range(l.startMinutes, l.endMinutes)
            tvTitle.text = l.title
            tvType.text = l.type
            tvRoom.text = "📍 " + Fmt.room(l.room)
            tvTeacher.text = "👨‍🏫 " + l.teacher
            root.setOnClickListener { onClick(l) }
        }
    }
}

class TaskAdapter(
    private val onToggle: (com.example.lab4_ui_components.data.Task) -> Unit,
    private val onEdit: (com.example.lab4_ui_components.data.Task) -> Unit,
    private val onDelete: (com.example.lab4_ui_components.data.Task) -> Unit
) : ListAdapter<TaskRow, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<TaskRow>() {
    override fun areItemsTheSame(a: TaskRow, b: TaskRow) = when {
        a is TaskRow.Header && b is TaskRow.Header -> a.title == b.title
        a is TaskRow.Item && b is TaskRow.Item -> a.task.id == b.task.id
        else -> false
    }

    override fun areContentsTheSame(a: TaskRow, b: TaskRow) = a == b
}) {
    class HeaderVH(val b: ItemHeaderBinding) : RecyclerView.ViewHolder(b.root)
    class ItemVH(val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemViewType(position: Int) = if (getItem(position) is TaskRow.Header) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == 0) HeaderVH(ItemHeaderBinding.inflate(inf, parent, false))
        else ItemVH(ItemTaskBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is TaskRow.Header -> (holder as HeaderVH).b.tvHeader.text = row.title
            is TaskRow.Item -> bindTask((holder as ItemVH).b, row.task)
        }
    }

    private fun bindTask(b: ItemTaskBinding, t: com.example.lab4_ui_components.data.Task) {
        val ctx = b.root.context
        b.tvTitle.text = t.title
        b.tvTitle.paintFlags = if (t.done) b.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else b.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        b.tvTitle.setTextColor(ctx.getColor(if (t.done) R.color.muted else R.color.text_primary))
        val overdue = !t.done && t.dueAt < System.currentTimeMillis()
        b.tvSub.text = if (t.done) "Выполнено" else Fmt.dateTimeLabel(t.dueAt)
        b.tvSub.setTextColor(ctx.getColor(if (overdue) R.color.red else R.color.muted))
        b.check.isActivated = t.done
        b.checkIcon.isVisible = t.done
        b.checkArea.setOnClickListener { onToggle(t) }
        b.root.setOnClickListener { onEdit(t) }
        b.root.setOnLongClickListener { onDelete(t); true }
    }
}

class NewsAdapter : ListAdapter<News, NewsAdapter.VH>(object : DiffUtil.ItemCallback<News>() {
    override fun areItemsTheSame(a: News, b: News) = a.id == b.id
    override fun areContentsTheSame(a: News, b: News) = a == b
}) {
    class VH(val b: ItemNewsBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = getItem(position)
        val d = LocalDate.ofEpochDay(n.date)
        holder.b.tvBadge.text = if (d == LocalDate.now()) "Сегодня" else Fmt.dayMonth(d)
        holder.b.tvTitle.text = n.title
        holder.b.tvText.text = n.text
    }
}

class NotificationAdapter : ListAdapter<AppNotification, NotificationAdapter.VH>(
    object : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(a: AppNotification, b: AppNotification) = a.id == b.id
        override fun areContentsTheSame(a: AppNotification, b: AppNotification) = a == b
    }
) {
    class VH(val b: ItemNotificationBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = getItem(position)
        holder.b.tvTitle.text = n.title
        holder.b.tvText.text = n.text
        holder.b.tvTime.text = Fmt.dateTimeLabel(n.timestamp)
    }
}
