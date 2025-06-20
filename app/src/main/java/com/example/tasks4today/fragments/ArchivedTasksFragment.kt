package com.example.tasks4today.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasks4today.R
import com.example.tasks4today.databinding.FragmentArchivedTasksBinding
import com.example.tasks4today.utils.ArchivedTaskAdapter
import com.example.tasks4today.utils.T4TData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ArchivedTasksFragment : Fragment(), ArchivedTaskAdapter.ArchivedTaskAdapterClicksInterface {

    private lateinit var binding: FragmentArchivedTasksBinding
    private lateinit var adapter: ArchivedTaskAdapter
    private var tasksList = mutableListOf<T4TData>()
    private lateinit var db: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    // Método para inflar el layout del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentArchivedTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Método que se ejecuta cuando la vista está completamente creada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización de eventos del menú y ajustes
        menuEvents()
        settingsEvents()

        // Inicialización del NavController
        navController = Navigation.findNavController(view)

        // Configuración del RecyclerView
        binding.recyclerViewArchivadas.layoutManager = LinearLayoutManager(context)
        adapter = ArchivedTaskAdapter(tasksList)
        adapter.setListener(this)
        binding.recyclerViewArchivadas.adapter = adapter

        // Inicialización de Firebase Authentication y Database
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().getReference("ArchivedTasks")
            .child(auth.currentUser?.uid.toString())

        // Cargar las tareas archivadas desde Firebase
        loadArchivedTasks()

        // Log para verificar qué tareas están en tasksList
        Log.d("TasksList", "List size: ${tasksList.size}")
        tasksList.forEach { task ->
            Log.d("TaskListItem", "Task: ${task.task}, Status: ${task.status}")
        }
    }

    // Método para eliminar una tarea
    override fun onDeleteTask(t4TData: T4TData) {
        Log.d("DeleteTask", "Attempting to delete task with ID: ${t4TData.taskId}")
        db.child(t4TData.taskId).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("DeleteTask", "Task deleted successfully")
                Toast.makeText(context, "Tarea borrada", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("DeleteTask", "Failed to delete task: ${it.exception?.message}")
                Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("DeleteTask", "Error during task deletion: ${e.message}")
            Toast.makeText(context, "Error al borrar la tarea", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para cargar las tareas archivadas desde Firebase
    private fun loadArchivedTasks() {
        Log.d("FirebaseData", "Loading archived tasks from Firebase...")
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tasksList.clear()
                Log.d("FirebaseData", "Total tasks: ${snapshot.childrenCount}")

                for (taskSnapshot in snapshot.children) {
                    val taskTitle = taskSnapshot.child("task").value?.toString() ?: "No task"
                    val taskDescription =
                        taskSnapshot.child("description").value?.toString() ?: "No description"
                    val taskPriority =
                        taskSnapshot.child("priority").value?.toString() ?: "No priority"
                    val taskStatus = taskSnapshot.child("status").value?.toString() ?: "incomplete"
                    val taskId = taskSnapshot.key ?: "No ID"

                    // Log para verificar los datos obtenidos
                    Log.d(
                        "FirebaseData",
                        "Task: $taskTitle, Description: $taskDescription, Priority: $taskPriority, Status: $taskStatus"
                    )

                    val task = T4TData(
                        taskId = taskId,
                        task = taskTitle,
                        description = taskDescription,
                        priority = taskPriority,
                        status = taskStatus
                    )

                    tasksList.add(task)
                }

                Log.d("FirebaseData", "Tasks loaded: ${tasksList.size}")
                adapter.notifyDataSetChanged()
            }

            // Manejo de errores en la carga de datos
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseData", "Error loading tasks: ${error.message}")
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Método para configurar los eventos del botón de configuración
    private fun settingsEvents() {
        val settingsButton = binding.settingsBotonArchived
        settingsButton.setOnClickListener {
            Log.d("SettingsEvent", "Settings button clicked")
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.pop_up_layout_settings, null)
            val popupWindow = PopupWindow(
                popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            popupWindow.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(), R.color.azul_fondo
                    )
                )
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            popupWindow.showAsDropDown(settingsButton)

            val logoutButton = binding.settingsBotonArchived
            logoutButton.setOnClickListener {
                Log.d("SettingsEvent", "Logout button clicked")
                logout()
                popupWindow.dismiss()
            }
        }
    }

    // Método para cerrar sesión
    private fun logout() {
        Log.d("Logout", "Logging out user...")
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(context, "Cerrando sesión", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_achievedTasksFragment_to_signInFragment)
    }

    // Método para configurar los eventos del botón de menú
    private fun menuEvents() {
        val menuButton = binding.menuBotonArchived
        menuButton.setOnClickListener {
            Log.d("MenuEvent", "Menu button clicked")
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_layout_menu_from_archived_tasks, null)
            val popupWindow = PopupWindow(
                popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            popupWindow.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(), R.color.azul_fondo
                    )
                )
            )
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
            popupWindow.showAsDropDown(menuButton)

            val actualTasksButton = popupView.findViewById<TextView>(R.id.menu_actual_tasks)
            val statisticsButton = popupView.findViewById<TextView>(R.id.menu_statistics2)

            actualTasksButton.setOnClickListener {
                Log.d("MenuEvent", "Actual tasks button clicked")
                showActualTasks()
                popupWindow.dismiss()
            }

            statisticsButton.setOnClickListener {
                Log.d("MenuEvent", "Statistics button clicked")
                showStatistics()
                popupWindow.dismiss()
            }
        }
    }

    // Método para mostrar las tareas actuales
    private fun showActualTasks() {
        Log.d("Navigation", "Navigating to actual tasks")
        Toast.makeText(context, "Mostrando tareas actuales", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_achievedTasksFragment_to_homeFragment)
    }

    // Método para mostrar las estadísticas
    private fun showStatistics() {
        Log.d("Navigation", "Navigating to statistics")
        Toast.makeText(context, "Mostrando estadísticas", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_achievedTasksFragment_to_estadisticasFragment)
    }
}
