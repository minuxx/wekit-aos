package com.coconutplace.wekit.data.remote.interest.listeners

import com.coconutplace.wekit.data.entities.Auth
import com.coconutplace.wekit.data.entities.Home

interface InterestListener {

    fun onInterestStarted()
    fun onInterestSuccess()
    fun onInterestFailure(code: Int, message: String)

}