package com.example.tasks4today.utils

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasks4today.R
import com.example.tasks4today.databinding.EachTodoItemBinding

class T4TAdapter(private var list: MutableList<T4TData>) :

    RecyclerView.Adapter<T4TAdapter.TodoViewHolder>() {


    private var listener: T4TAdapaterClicksInterface? = null
    fun setListener(listener: T4TAdapaterClicksInterface) {

        this.listener = listener
    }


    inner class TodoViewHolder(val binding: EachTodoItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding =
            EachTodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface T4TAdapaterClicksInterface {

        fun onDeleteTask(t4TData: T4TData)
        fun onEditTaskBtnClicked(t4TData: T4TData)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        with(holder) {

            with(list[position]) {


                // Asignar el nombre de la tarea al TextView
                binding.todoTask.text = this.task

                // Cambiar el color del texto según la categoría

                when (this.priority) {
                    "Alta" -> {
                        binding.todoTaskContainerShadow.setBackgroundColor(
                            binding.root.context.getColor(
                                R.color.rojo_fondo
                            )
                        )   // Color rojo para tareas de alta prioridad
                    }

                    "Media" -> {
                        binding.todoTaskContainerShadow.setBackgroundColor(
                            binding.root.context.getColor(
                                R.color.amarillo_fondo
                            )
                        ) // Color amarillo para tareas de media prioridad
                    }

                    else -> {

                        binding.todoTaskContainerShadow.setBackgroundColor(
                            binding.root.context.getColor(
                                R.color.verde_fondo
                            )
                        ) // Fondo transparente (o cualquier otro color predeterminado)
                    }
                }



                binding.editTask.setOnClickListener {

                    listener?.onEditTaskBtnClicked(this)
                }


            }

        }


    }

}
