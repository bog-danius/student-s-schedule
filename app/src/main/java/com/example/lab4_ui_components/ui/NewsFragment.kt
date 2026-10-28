package com.example.lab4_ui_components.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.lab4_ui_components.R
import com.example.lab4_ui_components.databinding.FragmentNewsBinding
import com.example.lab4_ui_components.network.NewsApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class NewsFragment :
    Fragment(R.layout.fragment_news) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        val b = FragmentNewsBinding.bind(view)

        b.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val adapter = ApiNewsAdapter()
        b.rvNews.adapter = adapter

        // Состояние загрузки
        fun showLoading() {
            b.loadingContainer.isVisible = true
            b.errorContainer.isVisible = false
            b.rvNews.isVisible = false
        }

        // Состояние ошибки
        fun showError(message: String) {
            b.loadingContainer.isVisible = false
            b.rvNews.isVisible = false
            b.errorContainer.isVisible = true
            b.tvErrorMessage.text = message
        }

        // Получение данных с сервера
        fun loadNews() {
            showLoading()

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        NewsApiClient.service.getTopNews(
                            limit = 20
                        )
                    }

                    if (response.hits.isEmpty()) {
                        showError(
                            "Сервер вернул пустой список. " +
                                "Попробуйте обновить данные."
                        )
                    } else {
                        adapter.submitList(response.hits)

                        b.loadingContainer.isVisible = false
                        b.errorContainer.isVisible = false
                        b.rvNews.isVisible = true
                    }

                } catch (cancelled: CancellationException) {
                    throw cancelled

                } catch (error: IOException) {
                    showError(
                        "Не удалось подключиться к серверу. " +
                            "Проверьте интернет и повторите попытку."
                    )

                } catch (error: HttpException) {
                    showError(
                        "Ошибка сервера: код ${error.code()}. " +
                            "Попробуйте позже."
                    )

                } catch (error: Exception) {
                    showError(
                        "Не удалось обработать ответ сервера. " +
                            "Попробуйте ещё раз."
                    )
                }
            }
        }

        // Повторная загрузка после ошибки
        b.btnRetry.setOnClickListener {
            loadNews()
        }

        // Автоматическая загрузка при открытии экрана
        loadNews()
    }
}
