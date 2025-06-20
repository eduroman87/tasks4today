package com.example.tasks4today.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasks4today.R
import com.example.tasks4today.databinding.EachArchivedItemBinding
import com.example.tasks4today.utils.T4TData


class ArchivedTaskAdapter(private var list: MutableList<T4TData>) :
    RecyclerView.Adapter<ArchivedTaskAdapter.TaskViewHolder>() {

    private var listener: ArchivedTaskAdapterClicksInterface? = null

    fun setListener(listener: ArchivedTaskAdapterClicksInterface) {
        this.listener = listener
    }

    inner class TaskViewHolder(val binding: EachArchivedItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = EachArchivedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        Log.d("Adapter", "onCreateViewHolder called")
        return TaskViewHolder(binding)
    }

    override fun getItemCount(): Int {
        Log.d("Adapter", "getItemCount called, size: ${list.size}")
        return list.size
    }

    interface ArchivedTaskAdapterClicksInterface {
        fun onDeleteTask(t4TData: T4TData)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = list[position]
        Log.d("Adapter", "onBindViewHolder called for position: $position, task: ${task.task}")

        with(holder.binding) {
            todoTask.text = task.task.ifEmpty { "No task available" }

            deleteBotonArchivedTask.setOnClickListener {
                Log.d("AdapterClick", "Delete clicked for task: ${task.task}")
                listener?.onDeleteTask(task)
            }
        }
    }


}
