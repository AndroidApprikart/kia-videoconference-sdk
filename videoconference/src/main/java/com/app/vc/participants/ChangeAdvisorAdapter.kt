package com.app.vc.participants

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.R
import com.app.vc.models.AdvisorModel

class ChangeAdvisorAdapter(
    private val list: ArrayList<AdvisorModel>,
    private val onItemSelected: (AdvisorModel) -> Unit
) : RecyclerView.Adapter<ChangeAdvisorAdapter.ViewHolder>() {

    private var selectedPosition = -1

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val txtInitial: TextView =
            itemView.findViewById(R.id.txtInitial)

        val name: TextView =
            itemView.findViewById(R.id.tctParticipantName)

        val radioButton: RadioButton =
            itemView.findViewById(R.id.radiobutton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.manage_participants, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {

        val item = list[position]

        holder.name.text = item.name

        // Initial letter
        val initial = item.name.trim()
            .firstOrNull()
            ?.toString()
            ?.uppercase()

        holder.txtInitial.text = initial ?: "?"

        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onItemSelected(item)
        }

        holder.radioButton.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged()
            onItemSelected(item)
        }
    }
}