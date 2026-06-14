package com.reminder.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/**
 * 인자가 있는 ViewModel을 Hilt 없이 간단히 생성하기 위한 헬퍼.
 */
inline fun <reified VM : ViewModel> simpleViewModelFactory(crossinline create: () -> VM) =
    viewModelFactory {
        initializer { create() }
    }
