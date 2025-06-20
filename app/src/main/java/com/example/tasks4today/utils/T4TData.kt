package com.example.tasks4today.utils

data class T4TData( //Valores que introducimos en la base de datos de firebase
    val taskId: String = "",        // ID de la tarea
    var task: String = "",          // Título de la tarea
    var description: String = "",   // Descripción de la tarea
    var category: String = "", //Categoría de la tarea
    var priority: String = "",  // Prioridad de la tarea
    var status: String = "incomplete" // Estado de la tarea
)
