package com.example.tasks4today.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.tasks4today.R
import com.example.tasks4today.databinding.FragmentAddTodoPopupBinding
import com.example.tasks4today.utils.T4TData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddTodoPopupFragment : DialogFragment() {

    private lateinit var binding: FragmentAddTodoPopupBinding
    private lateinit var listener: DialogNextBtnClickListerner
    private var t4tData: T4TData? = null
    private val auth = FirebaseAuth.getInstance()

    // Función para establecer el listener para eventos de botones
    fun setListener(listener: DialogNextBtnClickListerner) {
        this.listener = listener
    }

    companion object {
        const val TAG = "AddTodoPopUpFragment"

        // Función para crear una nueva instancia del fragment con los argumentos correspondientes
        @JvmStatic
        fun newInstance(
            taskId: String,
            task: String,
            description: String,
            category: String,
            priority: String,
            status: String
        ) = AddTodoPopupFragment().apply {
            arguments = Bundle().apply {
                putString("taskId", taskId)
                putString("task", task)
                putString("description", description)
                putString("category", category)
                putString("priority", priority)
                putString("status", status)
            }
        }
    }

    // Función para crear la vista del fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creando vista del fragmento.")
        binding = FragmentAddTodoPopupBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Función que se llama después de crear la vista, para inicializar el contenido
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Fragment creado y vista inicializada.")

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            Log.d("Auth", "Usuario autenticado: ${currentUser.uid}")
            cargarCategoriasDesdeFirebase() // Cargamos las categorías desde Firebase
        } else {
            Log.e("Auth", "El usuario no está autenticado.")
            Toast.makeText(context, "Por favor, inicia sesión.", Toast.LENGTH_SHORT).show()
        }

        configurarSpinnerPrioridades() // Configuramos el spinner para seleccionar prioridad
        configurarVistaConArgumentos() // Configuramos la vista con los datos pasados como argumentos
        registerEvents() // Registramos los eventos de los botones
    }

    // Función para configurar el spinner de prioridades
    private fun configurarSpinnerPrioridades() {
        Log.d(TAG, "Configurando Spinner de prioridades.")
        val prioritySpinner: Spinner = binding.todoPrioritySpinner
        val adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.prioridades,  // El array con las prioridades
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        prioritySpinner.adapter = adapter
        Log.d(TAG, "Spinner de prioridades configurado correctamente.")
    }

    // Función para configurar la vista con los datos recibidos a través de los argumentos
    private fun configurarVistaConArgumentos() {
        if (arguments != null) {
            Log.d(TAG, "Recuperando datos de los argumentos.")
            t4tData = T4TData(
                arguments?.getString("taskId").toString(),
                arguments?.getString("task").toString(),
                arguments?.getString("description").toString(),
                arguments?.getString("category").toString(),
                arguments?.getString("priority").toString(),
                arguments?.getString("status").toString()
            )
            Log.d(TAG, "Datos obtenidos de argumentos: $t4tData")

            binding.todoEt.setText(t4tData?.task)
            binding.todoDescriptionEt.setText(t4tData?.description)
            binding.todoPrioritySpinner.setSelection(getPriorityIndex(t4tData?.priority))
            binding.todoCategoryEt.setText(t4tData?.category)
        } else {
            Log.w(TAG, "No se encontraron argumentos para configurar la vista.")
        }
    }

    // Función para cargar las categorías desde Firebase
    private fun cargarCategoriasDesdeFirebase() {
        Log.d("Firebase", "Cargando categorías desde Firebase.")
        val allCategories = mutableListOf<String>()
        val currentUser = auth.currentUser
        if (!isAdded) {
            Log.e("Firebase", "Fragmento no añadido. Deteniendo carga de categorías.")
            return
        }
        if (currentUser == null) {
            Log.e("Firebase", "Usuario no autenticado. No se pueden cargar las categorías.")
            return
        }

        val userId = currentUser.uid
        val tasksRef = FirebaseDatabase.getInstance().getReference("Tasks").child(userId)
        val archivedTasksRef =
            FirebaseDatabase.getInstance().getReference("ArchivedTasks").child(userId)

        // Escuchamos los datos de las tareas activas
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("Firebase", "Datos de Tasks obtenidos: ${snapshot.childrenCount} elementos.")
                snapshot.children.forEach { child ->
                    val category = child.child("category").getValue(String::class.java)
                    if (category != null && !allCategories.contains(category)) {
                        allCategories.add(category)
                        Log.d("Firebase", "Categoría añadida desde Tasks: $category")
                    }
                }

                // Escuchamos los datos de las tareas archivadas
                archivedTasksRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(archivedSnapshot: DataSnapshot) {
                        Log.d(
                            "Firebase",
                            "Datos de ArchivedTasks obtenidos: ${archivedSnapshot.childrenCount} elementos."
                        )
                        archivedSnapshot.children.forEach { child ->
                            val category = child.child("category").getValue(String::class.java)
                            if (category != null && !allCategories.contains(category)) {
                                allCategories.add(category)
                                Log.d(
                                    "Firebase", "Categoría añadida desde ArchivedTasks: $category"
                                )
                            }
                        }

                        val uniqueCategories = allCategories.distinct()
                        Log.d("Firebase", "Categorías únicas: $uniqueCategories")
                        if (isAdded) {
                            if (uniqueCategories.isNotEmpty()) {
                                val categoryAdapter = ArrayAdapter(
                                    requireContext(),
                                    R.layout.popup_category_array,
                                    uniqueCategories
                                )
                                binding.todoCategoryEt.setAdapter(categoryAdapter)
                            }
                            Log.d("Firebase", "Adaptador de categorías configurado correctamente.")
                        } else {
                            Log.e("Firebase", "No se encontraron categorías únicas.")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "Firebase",
                            "Error al leer categorías de ArchivedTasks",
                            error.toException()
                        )
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer categorías desde Tasks", error.toException())
            }
        })
    }

    // Función para registrar los eventos de los botones
    private fun registerEvents() {
        binding.todoNextBtn.setOnClickListener {
            val task = binding.todoEt.text.toString()
            val description = binding.todoDescriptionEt.text.toString()
            val category = binding.todoCategoryEt.text.toString()
            val priority = binding.todoPrioritySpinner.selectedItem.toString()

            Log.d(
                TAG,
                "Botón 'Siguiente' presionado. Datos introducidos: task=$task, description=$description, category=$category, priority=$priority"
            )

            if (task.isNotEmpty()) {
                if (t4tData == null) {
                    listener.onSaveTask(task, description, category, priority, binding.todoEt)
                    Log.d(TAG, "Nueva tarea guardada: $task")
                } else {
                    t4tData?.apply {
                        this.task = task
                        this.description = description
                        this.category = category
                        this.priority = priority
                    }
                    listener.onUpdateTask(t4tData!!, binding.todoEt)
                    Log.d(TAG, "Tarea actualizada: $t4tData")
                }
            } else {
                Log.w(TAG, "El campo de tarea está vacío.")
                Toast.makeText(context, "Por favor, introduce una tarea", Toast.LENGTH_SHORT).show()
            }
        }
        binding.todoClose.setOnClickListener {
            Log.d(TAG, "Botón 'Cerrar' presionado.")
            dismiss()
        }
    }

    // Función para obtener el índice de la prioridad seleccionada
    private fun getPriorityIndex(priority: String?): Int {
        val priorities = resources.getStringArray(R.array.prioridades)
        return priorities.indexOf(priority).also {
            Log.d(TAG, "Índice de prioridad obtenido: $it para la prioridad '$priority'")
        }
    }

    // Interfaz para manejar el clic en los botones del diálogo
    interface DialogNextBtnClickListerner {
        fun onSaveTask(
            task: String, description: String, category: String, priority: String, todoEt: EditText
        )

        fun onUpdateTask(t4tData: T4TData, todoEt: EditText)
    }
}
