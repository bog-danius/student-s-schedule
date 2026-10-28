package com.example.lab4_ui_components.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4_ui_components.databinding.ItemNewsBinding
import com.example.lab4_ui_components.network.ApiNews

// Адаптер для отображения публикаций из API
class ApiNewsAdapter :
    ListAdapter<ApiNews, ApiNewsAdapter.NewsViewHolder>(
        DiffCallback
    ) {

    class NewsViewHolder(
        val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NewsViewHolder {

        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NewsViewHolder,
        position: Int
    ) {
        val item = getItem(position)

        val title = item.title
            ?.takeIf { it.isNotBlank() }
            ?: item.storyTitle
                ?.takeIf { it.isNotBlank() }
            ?: "Публикация без заголовка"

        val meta = buildList {
            item.createdAt?.take(10)?.let {
                add(it)
            }

            item.author
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    add("Автор: $it")
                }
        }.joinToString(" • ")

        with(holder.binding) {
            tvBadge.text =
                meta.ifBlank { "Hacker News" }

            tvTitle.text =
                title.replaceFirstChar { it.uppercase() }

            tvText.text =
                item.storyText?.takeIf {
                    it.isNotBlank()
                } ?: "Публикация из ленты Hacker News. " +
                    "Источник и обсуждение доступны на сайте Hacker News."
        }
    }

    private companion object {

        val DiffCallback =
            object : DiffUtil.ItemCallback<ApiNews>() {

                override fun areItemsTheSame(
                    oldItem: ApiNews,
                    newItem: ApiNews
                ): Boolean {
                    return oldItem.objectId ==
                        newItem.objectId
                }

                override fun areContentsTheSame(
                    oldItem: ApiNews,
                    newItem: ApiNews
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
