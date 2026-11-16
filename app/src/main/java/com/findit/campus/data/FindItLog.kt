package com.findit.campus.data

import android.util.Log

object FindItLog {
    private const val TAG = "FindIt"

    fun i(message: String) = Log.i(TAG, message)

    fun d(message: String) = Log.d(TAG, message)

    fun e(message: String, error: Throwable? = null) = Log.e(TAG, message, error)
}
