package com.example.tasks4today.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.tasks4today.R
import com.example.tasks4today.databinding.FragmentEstadisticasBinding
import com.example.tasks4today.utils.T4TData
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StatisticsFragment : Fragment() {

    // Inicialización de variables
    private lateinit var etNumeroTareasActivas: EditText
    private lateinit var etNumeroTareasArchivadas: EditText
    private lateinit var etNumeroTareasAlta: EditText
    private lateinit var etNumeroTareasMedia: EditText
    private lateinit var etNumeroTareasBaja: EditText

    private lateinit var pieChart: PieChart
    private lateinit var legendContainer: LinearLayout
    private val categoryColors: MutableMap<String, Int> = mutableMapOf()
    private val database = FirebaseDatabase.getInstance()
    private val tasksRef = database.getReference("Tasks")
    private val archivedTasksRef = database.getReference("ArchivedTasks")
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.i("StatisticsFragment", "Fragment creado. Iniciando binding y consultas.")
        val binding = FragmentEstadisticasBinding.inflate(inflater, container, false)

        // Inicializa los EditText
        etNumeroTareasActivas = binding.etNumeroTareasActivas
        etNumeroTareasArchivadas = binding.etNumeroTareasArchivadas
        etNumeroTareasAlta = binding.etNumeroTareasAlta
        etNumeroTareasMedia = binding.etNumeroTareasMedia
        etNumeroTareasBaja = binding.etNumeroTareasBaja

        // Inicializa el PieChart y el contenedor de leyenda
        pieChart = binding.pieChart


        // Inicializa el NavController
        navController = requireActivity().findNavController(R.id.nav_host_fragment)

        // Llama a las funciones para cargar las estadísticas
        loadTaskStatistics()

        // Configuración de eventos del menú y ajustes
        menuEvents(binding)
        settingsEvents(binding)

        return binding.root
    }

    private fun loadTaskStatistics() {
        Log.i("StatisticsFragment", "Iniciando carga de estadísticas.")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("StatisticsFragment", "Usuario no autenticado. No se pueden cargar estadísticas.")
            return
        }

        val userId = currentUser.uid
        val tasksRef = database.getReference("Tasks").child(userId)
        val archivedTasksRef = database.getReference("ArchivedTasks").child(userId)

        var activeTasksCount = 0
        var archivedTasksCount = 0
        var highPriorityTasksCount = 0
        var mediumPriorityTasksCount = 0
        var lowPriorityTasksCount = 0
        var categoryCounts = mutableMapOf<String, Int>()


        // Consultar tareas activas
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.i(
                        "StatisticsFragment",
                        "Datos de tareas activas encontrados: ${snapshot.childrenCount}"
                    )
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(T4TData::class.java)
                        task?.let {
                            activeTasksCount++
                            when (it.priority) {
                                "Alta" -> highPriorityTasksCount++
                                "Media" -> mediumPriorityTasksCount++
                                "Baja" -> lowPriorityTasksCount++
                            }
                            val category = it.category
                            categoryCounts[category] = categoryCounts.getOrDefault(category, 0) + 1
                        }
                    }
                    etNumeroTareasActivas.setText(activeTasksCount.toString())
                    etNumeroTareasAlta.setText(highPriorityTasksCount.toString())
                    etNumeroTareasMedia.setText(mediumPriorityTasksCount.toString())
                    etNumeroTareasBaja.setText(lowPriorityTasksCount.toString())

                    // Preparar el gráfico
                    preparePieChart(categoryCounts)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StatisticsFragment", "Error al leer las tareas: ${error.message}")
            }
        })

        // Consultar tareas archivadas
        archivedTasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    archivedTasksCount = snapshot.childrenCount.toInt()
                    etNumeroTareasArchivadas.setText(archivedTasksCount.toString())
                    Log.i(
                        "StatisticsFragment",
                        "Datos de tareas archivadas encontrados: $archivedTasksCount"
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StatisticsFragment", "Error al leer las tareas archivadas: ${error.message}")
            }
        })
    }


    // Preparar y configurar el gráfico circular (PieChart)
    private fun preparePieChart(categoryCounts: Map<String, Int>) {
        val entries = mutableListOf<PieEntry>()




        categoryCounts.forEach { (category, count) ->
            // Asigna un color aleatorio a la categoría
            val randomColor = generateRandomColor()
            categoryColors[category] = randomColor

            // Añadir la categoría y su número al gráfico
            entries.add(PieEntry(count.toFloat(), category))


        }

        setUpPieChart(entries)
    }

    // Configurar el PieChart
    private fun setUpPieChart(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "") // Categorías de las tareas
        dataSet.colors = entries.map { categoryColors[it.label] ?: Color.GRAY } // Asignar colores
        dataSet.valueTextSize = 0f
        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.invalidate() // Refrescar el gráfico
        pieChart.description.isEnabled = false
        // Cambiar el color de fondo del centro del PieChart
        val centerBackgroundColor = ContextCompat.getColor(requireContext(), R.color.azul_fondo)
        // Cargar la fuente Montserrat
        val montserratTypeface = ResourcesCompat.getFont(requireContext(), R.font.montserrat)
        dataSet.valueTypeface = montserratTypeface
        // Usar PercentFormatter para mostrar los porcentajes


        val customFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%" // Formato sin decimales, solo el valor entero seguido de "%"
            }
        }
        dataSet.valueFormatter = customFormatter
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = resources.getColor(android.R.color.white)
        // Activar la visualización de los porcentajes

        pieChart.setUsePercentValues(true) // Usar valores en porcentaje
        pieChart.setHoleColor(centerBackgroundColor)
        pieChart.setDrawHoleEnabled(true)
    }

    // Generar un color aleatorio
    private fun generateRandomColor(): Int {
        val rand = (0..255).random()
        return Color.rgb(rand, (0..255).random(), (0..255).random())
    }

    // Eventos del menú
    private fun menuEvents(binding: FragmentEstadisticasBinding) {
        val menuButton = binding.menuBotonEstadisticas

        menuButton.setOnClickListener {
            val inflater = LayoutInflater.from(requireContext())
            val popupView =
                inflater.inflate(R.layout.popup_layout_menu_from_estadisticas_fragment, null)
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

            val archivedTasksButton =
                popupView.findViewById<TextView>(R.id.menu_archived_tasks_from_estadisticas)
            val actualButton =
                popupView.findViewById<TextView>(R.id.menu_actual_tasks_from_estadisticas)

            archivedTasksButton.setOnClickListener {
                showArchivedTasks()
                popupWindow.dismiss()
            }

            actualButton.setOnClickListener {
                showActualTasks()
                popupWindow.dismiss()
            }
        }
    }

    // Función para mostrar tareas archivadas
    private fun showArchivedTasks() {
        Toast.makeText(context, "Mostrando tareas archivadas", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_estadisticasFragment_to_achievedTasksFragment)
    }

    // Función para mostrar tareas actuales
    private fun showActualTasks() {
        Toast.makeText(context, "Mostrando tareas actuales", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_estadisticasFragment_to_homeFragment)
    }

    // Eventos de los ajustes
    private fun settingsEvents(binding: FragmentEstadisticasBinding) {
        val settingsButton = binding.settingsBotonEstadisticas

        settingsButton.setOnClickListener {
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

            val logoutButton = popupView.findViewById<TextView>(R.id.menu_logout)
            logoutButton.setOnClickListener {
                logout()
                popupWindow.dismiss()
            }
        }
    }

    // Función para cerrar sesión
    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(context, "Cerrando sesión", Toast.LENGTH_SHORT).show()
        navController.navigate(R.id.action_estadisticasFragment_to_signInFragment)
    }
}
