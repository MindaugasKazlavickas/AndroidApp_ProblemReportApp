package com.example.problemreportapp

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val TAG = "ApiClient"

    // Initialize Retrofit
    private val retrofit: Retrofit by lazy {
        try {
            Log.d(TAG, "Initializing Retrofit client...")
            Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5001/report/") // Emulator to localhost mapping
                .addConverterFactory(GsonConverterFactory.create())
                .build().also {
                    Log.d(TAG, "Retrofit client initialized successfully.")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Retrofit: ${e.message}")
            throw e // Optionally rethrow to halt if critical
        }
    }

    // Initialize API interface
    val api: ProblemReportApi by lazy {
        try {
            Log.d(TAG, "Creating API interface...")
            retrofit.create(ProblemReportApi::class.java).also {
                Log.d(TAG, "API interface created successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating API interface: ${e.message}")
            throw e
        }
    }
}
