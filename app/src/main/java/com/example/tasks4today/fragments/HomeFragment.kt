package com.example.tasks4today.fragments

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tasks4today.R
import com.example.tasks4today.databinding.FragmentHomeBinding
import com.example.tasks4today.utils.T4TAdapter
import com.example.tasks4today.utils.T4TData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class HomeFragment : Fragment(), AddTodoPopupFragment.DialogNextBtnClickListerner,
    T4TAdapter.T4TAdapaterClicksInterface {

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var archiveRef: DatabaseReference
    private lateinit var navController: NavController
    private lateinit var binding: FragmentHomeBinding
    private var popUpFragment: AddTodoPopupFragment? = null
    private lateinit var adapter: T4TAdapter
    private lateinit var mList: MutableList<T4TData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference.child("Tasks")
            .child(auth.currentUser?.uid.toString())

        // Inicialización de la referencia para las tareas archivadas
        archiveRef = FirebaseDatabase.getInstance().reference.child("ArchivedTasks")
            .child(auth.currentUser?.uid.toString())

        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        getDataFromFirebase()
        registerEvents()
        moveDeleteAchieve()
        menuEvents()
        settingsEvents()


    }

    // Registramos los eventos
    private fun registerEvents() {
        binding.AddBotonHome.setOnClickListener {
            if (popUpFragment != null) childFragmentManager.beginTransaction()
                .remove(popUpFragment!!).commit()

            popUpFragment = AddTodoPopupFragment()
            popUpFragment!!.setListener(this)
            popUpFragment!!.show(
                childFragmentManager, AddTodoPopupFragment.TAG
            )
        }
    }

    //Configuramos el botón de menú
    private fun menuEvents() {
        val menuButton = binding.menuBotonHome // Obtener botón

        // Establecer el listener para el botón
        menuButton.setOnClickListener {
            // Inflar el layout personalizado para el PopupWindow
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.popup_layout_menu, null)

            // Crear el PopupWindow con el layout inflado
            val popupWindow = PopupWindow(
                popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Establecer el fondo y otras configuraciones
            popupWindow.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(), R.color.azul_fondo
                    )
                )
            )
            popupWindow.isOutsideTouchable =
                true  // Permitir cerrar al hacer clic fuera del PopupWindow
            popupWindow.isFocusable = true  // Hacer el PopupWindow interactivo

            // Mostrar el PopupWindow debajo del botón
            popupWindow.showAsDropDown(menuButton)

            // Establecer listeners para los botones del PopupWindow
            val archivedTasksButton = popupView.findViewById<TextView>(R.id.menu_archived_tasks)
            val statisticsButton = popupView.findViewById<TextView>(R.id.menu_statistics)


            archivedTasksButton.setOnClickListener {
                showArchivedTasks()
                popupWindow.dismiss()  // Cerrar el PopupWindow después de la selección
            }

            statisticsButton.setOnClickListener {
                showStatistics()
                popupWindow.dismiss()  // Cerrar el PopupWindow después de la selección
            }

        }
    }

    // Función para mostrar tareas archivadas
    private fun showArchivedTasks() {
        // Lógica para mostrar las tareas archivadas
        Toast.makeText(context, "Mostrando tareas archivadas", Toast.LENGTH_SHORT).show()

        // Obtener el NavController y navegar al fragmento de Tareas Archivadas

        navController.navigate(R.id.action_homeFragment_to_achievedTasksFragment)

    }

    // Función para mostrar estadísticas
    private fun showStatistics() {
        // Lógica para mostrar las estadísticas
        Toast.makeText(context, "Mostrando estadísticas", Toast.LENGTH_SHORT).show()

        navController.navigate(R.id.action_homeFragment_to_estadisticasFragment)
        //agregar nuevo fragment de estadísticas, y actualizar el nav aquí. En el nuevo fragment, actualizar botones también.
    }


    //Función para el botón de settings
    private fun settingsEvents() {
        val settingsButton = binding.settingsBotonHome // Obtener botón settings

        // Establecer el listener para el botón
        settingsButton.setOnClickListener {
            // Inflar el layout personalizado para el PopupWindow
            val inflater = LayoutInflater.from(requireContext())
            val popupView = inflater.inflate(R.layout.pop_up_layout_settings, null)

            // Crear el PopupWindow con el layout inflado
            val popupWindow = PopupWindow(
                popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )

            // Establecer el fondo y otras configuraciones
            popupWindow.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        requireContext(), R.color.azul_fondo
                    )
                )
            )
            popupWindow.isOutsideTouchable =
                true  // Permitir cerrar al hacer clic fuera del PopupWindow
            popupWindow.isFocusable = true  // Hacer el PopupWindow interactivo

            // Mostrar el PopupWindow debajo del botón
            popupWindow.showAsDropDown(settingsButton)

            // Establecer listeners para los botones del PopupWindow

            val logoutButton = popupView.findViewById<TextView>(R.id.menu_logout)



            logoutButton.setOnClickListener {
                logout()
                popupWindow.dismiss()  // Cerrar el PopupWindow después de la selección
            }

        }
    }

    // Función para cerrar sesión
    private fun logout() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut()

        // Mostrar un mensaje de confirmación
        Toast.makeText(context, "Cerrando sesión", Toast.LENGTH_SHORT).show()

        // Redirigir al fragmento de SignIn
        navController.navigate(R.id.action_homeFragment_to_signInFragment2)
    }

    // Inicializamos los componentes incluidos en el fragment
    private fun init(view: View) {
        navController = Navigation.findNavController(view)
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference.child("Tasks")
            .child(auth.currentUser?.uid.toString())

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mList = mutableListOf()
        adapter = T4TAdapter(mList)
        adapter.setListener(this)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mList.clear()
                for (taskSnapshot in snapshot.children) {
                    val todoTask = T4TData(
                        taskSnapshot.key.toString(),
                        taskSnapshot.child("task").value.toString(),
                        taskSnapshot.child("description").value.toString(),
                        taskSnapshot.child("category").value.toString(),
                        taskSnapshot.child("priority").value.toString(),
                        taskSnapshot.child("status").value.toString(),


                        )
                    if (todoTask != null) {
                        mList.add(todoTask)
                    }
                }
                adapter.notifyDataSetChanged()
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    //Función para borrar o archivar tareas mediante movimiento de izquiera a derecha

    private fun moveDeleteAchieve() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // Permite movimiento vertical
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Permite deslizamiento horizontal
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val startPosition = viewHolder.bindingAdapterPosition
                var endPosition = target.bindingAdapterPosition

                if (startPosition != endPosition) {
                    changeCardPosition(startPosition, endPosition)
                }
                return true
            }


            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val task = mList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Deslizar hacia la izquierda borra la tarea
                    onDeleteTask(task) //
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Deslizar hacia la derecha la tarea va al archivo
                    archiveTask(task)
                }
            }


            // Función para colorear el fondo al mover la tarjeta al guardar tarea o borrar tarea
            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                val itemView = viewHolder.itemView
                val paint = Paint()


                if (isCurrentlyActive) {
                    // Determina qué color utilizar según la dirección del desplazamiento
                    val colorResId = if (dX > 0) {
                        R.color.verde_tarea // Usar verde cuando se deslice a la derecha
                    } else if (dX < 0) {
                        R.color.rojo_tarea // Usar rojo cuando se deslice a la izquierda
                    } else {
                        R.color.azul_fondo // Usar azul fondo cuando se deslice en otra dirección.
                    }


                    val color = ContextCompat.getColor(itemView.context, colorResId)


                    paint.color = color

                    // Dibuja el fondo con el color elegido
                    c.drawRect(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat(),
                        paint
                    )
                }
                super.onChildDraw(
                    c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive
                )
            }
        }


        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }


    //Función para guardar tarea
    override fun onSaveTask(
        todo: String, description: String, category: String, priority: String, todoEt: EditText
    ) { //falta status y category


        // Verificar que los campos no estén vacíos
        if (todo.isNotEmpty() && description.isNotEmpty() && priority.isNotEmpty() && category.isNotEmpty()) {
            val taskId = databaseRef.push().key ?: return
            val taskData = hashMapOf(
                "taskId" to taskId,
                "task" to todo,
                "description" to description,
                "category" to category,
                "priority" to priority,
                "status" to "pendiente" // o el valor que corresponda
            )

            databaseRef.child(taskId).setValue(taskData).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Tarea guardada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
                }

                todoEt.text = null // Limpiar campo de texto
                popUpFragment!!.dismiss() // Cerrar popup
            }
        } else {
            Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //Función para actualizar una tarea ya guardada
    override fun onUpdateTask(t4TData: T4TData, todoEt: EditText) {
        // Verificar que los campos no estén vacíos
        if (t4TData.task.isNotEmpty() && t4TData.description.isNotEmpty() && t4TData.priority.isNotEmpty() && t4TData.category.isNotEmpty()) {
            // Crear un mapa con todos los valores a actualizar
            val taskUpdates = mapOf<String, Any>(
                "task" to t4TData.task,
                "description" to t4TData.description,
                "priority" to t4TData.priority,
                "category" to t4TData.category
            )

            // Actualizar todos los campos de una vez utilizando children
            databaseRef.child(t4TData.taskId).updateChildren(taskUpdates).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(context, "Tarea actualizada", Toast.LENGTH_SHORT).show()
                    getDataFromFirebase()  // Recarga los datos desde Firebase
                    adapter.notifyDataSetChanged()  // Notifica al adaptador para actualizar el RecyclerView
                } else {
                    Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
                }

                todoEt.text = null // Limpiar campo de texto
                popUpFragment!!.dismiss() // Cerrar popup
            }
        } else {
            Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT)
                .show()
        }
    }

    //Función para borrar una tarea definitivamente
    override fun onDeleteTask(t4TData: T4TData) {
        databaseRef.child(t4TData.taskId).removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Tarea borrada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    //Función para editar una tarea
    override fun onEditTaskBtnClicked(t4TData: T4TData) {
        if (popUpFragment != null) childFragmentManager.beginTransaction().remove(popUpFragment!!)
            .commit()

        popUpFragment = AddTodoPopupFragment.newInstance(
            t4TData.taskId,
            t4TData.task,
            t4TData.description,
            t4TData.category,
            t4TData.priority,
            t4TData.status
        )
        popUpFragment!!.setListener(this)
        popUpFragment!!.show(childFragmentManager, AddTodoPopupFragment.TAG)

        //Hacer aquí la función para tareas acabadas basándome en lo de arriba,
    }

    //Función para archivar una tarea
    private fun archiveTask(task: T4TData) {
        // Mover la tarea a "Archivo de tareas"
        val taskKey = archiveRef.push().key!!
        val taskData = hashMapOf(
            "taskId" to task.taskId,
            "task" to task.task,
            "description" to task.description,
            "category" to task.category,
            "priority" to task.priority,
            "status" to "archivada" // Cambiar el estado si es necesario
        )

        archiveRef.child(taskKey).setValue(taskData).addOnCompleteListener { archiveTask ->
            if (archiveTask.isSuccessful) {
                Log.d("FirebaseData", "Task archived: ${taskData}")

                mList.remove(task) //útlimos cambios, revisar 12/11
                adapter.notifyDataSetChanged()
                // Eliminar la tarea original en lugar de usar onDeleteTask
                databaseRef.child(task.taskId).removeValue().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        Toast.makeText(context, "Tarea archivada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, deleteTask.exception?.message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(context, archiveTask.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Función que permite cambiar de posición de tarjetas en el redcycler view
    private fun changeCardPosition(startPosition: Int, endPosition: Int) {
        // Verifica que las posiciones sean diferentes antes de mover
        if (startPosition != endPosition) {
            val tarea = mList[startPosition]
            mList.removeAt(startPosition)
            mList.add(endPosition, tarea)
            adapter.notifyItemMoved(startPosition, endPosition)


        }
    }


}
