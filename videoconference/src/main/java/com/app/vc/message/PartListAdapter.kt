package com.kia.vc.message

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.vc.PreferenceManager
import com.app.vc.R
import com.app.vc.VCConstants

class PartListAdapter(
    val mContext:  android.content.Context,
    var dataList: ArrayList<Part>,
    var parentPosition: Int,
    var checkboxSelectedListener: OnPartCheckboxSelectedListener,
    var isCheckboxVisible: Boolean?,
    var isCheckboxSelectable:Boolean?
):RecyclerView.Adapter<PartListAdapter.PartListViewHolder>() {


    class PartListViewHolder(viewItem: View):RecyclerView.ViewHolder(viewItem) {
        var partName: TextView = viewItem.findViewById(R.id.tv_part_name)
        var partQuantity: TextView = viewItem.findViewById(R.id.tv_quantity)

        var partPrice: TextView = viewItem.findViewById(R.id.tv_price)

        var isSelectedCheckbox: CheckBox = viewItem.findViewById(R.id.checkbox_is_selected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartListViewHolder {
        val v =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.message_estimation_list_item, parent, false)
        return PartListViewHolder(v)
    }

    override fun onBindViewHolder(holder: PartListViewHolder, position: Int) {
        var currentItem = dataList[position]

        holder.partName.text = currentItem.partDescription
        holder.isSelectedCheckbox.isChecked = currentItem.isSelected == "Y"

        if(isCheckboxVisible!=null) {
            if(!isCheckboxVisible!!) {
                holder.isSelectedCheckbox.visibility = View.GONE
            }
        }
        holder.isSelectedCheckbox.isClickable = isCheckboxSelectable == true

        holder.isSelectedCheckbox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("checkboxSelection: ", "onBindViewHolder: ")
            checkboxSelectedListener?.onPartCheckBoxClicked(parentPosition,position, isChecked,dataList)
        }
        holder.partQuantity.text = currentItem.quantity

        holder.partPrice.text = currentItem.totalPrice
        if(PreferenceManager.getuserType().equals(VCConstants.UserType.CUSTOMER.value)){
            holder.isSelectedCheckbox.visibility=View.VISIBLE
            holder.isSelectedCheckbox.isSelected = currentItem.isSelected == "Y"
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


    interface OnPartCheckboxSelectedListener {
        fun onPartCheckBoxClicked(parentPosition: Int, childPosition: Int, isSelected: Boolean, arrayList: ArrayList<Part>)
    }
}