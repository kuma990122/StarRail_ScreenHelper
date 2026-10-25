package com.screenhelper.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.screenhelper.bootstrap.AppContainer
import com.screenhelper.model.*
import com.screenhelper.presentation.ScreenViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(app: AppContainer) : ScreenViewModel(app) {
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    fun save(value: AppSettings) = action("配置已加密保存") {
        if (!value.demoMode) require(value.text.baseUrl.isNotBlank() && value.text.model.isNotBlank() && value.text.key.isNotBlank()) { "请填写文本 API 地址、模型名称和 Key" }
        app.settings.save(value)
    }
}
