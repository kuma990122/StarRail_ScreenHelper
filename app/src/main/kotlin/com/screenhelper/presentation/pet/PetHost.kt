package com.screenhelper.presentation.pet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PetAnchor(val centerX: Float, val centerY: Float)
data class PetHostState(val appVisible: Boolean = false, val homeAnchor: PetAnchor? = null)

/** Screen coordinates only: never retains an Activity or a Compose view. */
object PetHost {
    private val visibleOwners = mutableSetOf<Any>()
    private var anchorOwner: Any? = null
    private val mutable = MutableStateFlow(PetHostState())
    val state = mutable.asStateFlow()
    @Synchronized fun foreground(owner: Any, visible: Boolean) {
        if(visible) visibleOwners.add(owner) else visibleOwners.remove(owner)
        mutable.update { it.copy(appVisible = visibleOwners.isNotEmpty()) }
    }
    @Synchronized fun anchor(owner: Any, value: PetAnchor?) {
        if(value!=null) {
            anchorOwner=owner
            mutable.update { it.copy(homeAnchor=value) }
        } else if(anchorOwner===owner) {
            anchorOwner=null
            mutable.update { it.copy(homeAnchor=null) }
        }
    }
}
