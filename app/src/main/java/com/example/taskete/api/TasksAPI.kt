package com.example.taskete.api

import com.example.taskete.data.TaskResponse
import retrofit2.Call
import retrofit2.http.GET

interface TasksAPI {
    @GET("tasks")
    fun getTasks(): Call<ArrayList<TaskResponse>>

}