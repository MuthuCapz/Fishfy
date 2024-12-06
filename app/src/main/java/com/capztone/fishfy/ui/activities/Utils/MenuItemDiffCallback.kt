package com.capztone.fishfy.ui.activities.Utils

import androidx.recyclerview.widget.DiffUtil
import com.capztone.fishfy.ui.activities.models.MenuItem

class MenuItemDiffCallback(
    private val oldList: List<MenuItem>,
    private val newList: List<MenuItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].foodId == newList[newItemPosition].foodId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
