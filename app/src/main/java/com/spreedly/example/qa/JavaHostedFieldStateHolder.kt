package com.spreedly.example.qa

import com.spreedly.hostedfields.models.HostedFieldState
import com.spreedly.sdk.models.FormFieldType
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object JavaHostedFieldStateHolder {
    private val cardLatest = AtomicReference<HostedFieldState?>(null)
    private val cvvLatest = AtomicReference<HostedFieldState?>(null)
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    @JvmStatic
    fun update(state: HostedFieldState) {
        when (state.fieldType) {
            is FormFieldType.CARD -> cardLatest.set(state)
            is FormFieldType.CVV -> cvvLatest.set(state)
            else -> Unit
        }
        _revision.value += 1
    }

    @JvmStatic
    fun getCard(): HostedFieldState? = cardLatest.get()

    @JvmStatic
    fun getCvv(): HostedFieldState? = cvvLatest.get()

    @Deprecated("Use getCard() or getCvv()")
    @JvmStatic
    fun get(): HostedFieldState? = getCvv() ?: getCard()

    @JvmStatic
    fun clear() {
        cardLatest.set(null)
        cvvLatest.set(null)
        _revision.value += 1
    }
}
