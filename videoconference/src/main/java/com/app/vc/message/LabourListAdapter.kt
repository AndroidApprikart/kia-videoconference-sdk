package com.kia.vc.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.utils.PreferenceManager
import com.app.vc.R
import com.app.vc.utils.VCConstants


class LabourListAdapter(
    val mContext:  android.content.Context,
    var dataList: ArrayList<Labour>,
    var parentPosition: Int,
    var checkboxSelectedListener: OnLabourCheckboxSelectedListener?,
    var isCheckBoxVisible: Boolean?,
    var isCheckboxSelectable: Boolean?
):RecyclerView.Adapter<LabourListAdapter.LabrourListViewHolder>() {

    class LabrourListViewHolder(viewItem: View):RecyclerView.ViewHolder(viewItem) {
        var labourName: TextView = viewItem.findViewById(R.id.tv_part_name)
        var labourQuantity: TextView = viewItem.findViewById(R.id.tv_quantity)
        var labourPrice: TextView = viewItem.findViewById(R.id.tv_price)

        var isSelectedCheckbox: CheckBox = viewItem.findViewById(R.id.checkbox_is_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabrourListViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_estimation_list_item, parent, false)
        return LabrourListViewHolder(v)
    }

    override fun onBindViewHolder(holder: LabrourListViewHolder, position: Int) {
        var currentItem = dataList[position]

        holder.isSelectedCheckbox.isClickable = isCheckboxSelectable == true

        holder.labourName.text = currentItem.labourDescription
        holder.labourQuantity.text=currentItem.labourQuantity
        holder.labourPrice.text=currentItem.totalLabourCost

        if(isCheckBoxVisible!=null) {
            if(!isCheckBoxVisible!!) {
                holder.isSelectedCheckbox.visibility = View.GONE
            }
        }

        if(PreferenceManager.getuserType().equals(VCConstants.UserType.CUSTOMER.value)){
            holder.isSelectedCheckbox.visibility=View.VISIBLE
            holder.isSelectedCheckbox.isSelected = currentItem.isSelected == "Y"
        }
        holder.isSelectedCheckbox.isChecked = currentItem.isSelected == "Y"

        holder.isSelectedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("checkboxSelection: ", "onBindViewHolder: ")
            checkboxSelectedListener?.onLabourCheckboxClick(parentPosition,position, isChecked,dataList)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    interface OnLabourCheckboxSelectedListener {
        fun onLabourCheckboxClick(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Labour>)
    }
}
