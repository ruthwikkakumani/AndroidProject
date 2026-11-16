package com.findit.campus.data.local

import androidx.room.TypeConverter
import com.findit.campus.model.ItemStatus
import com.findit.campus.model.ItemType

class Converters {
    @TypeConverter
    fun itemTypeToString(value: ItemType): String = value.name

    @TypeConverter
    fun stringToItemType(value: String): ItemType = ItemType.valueOf(value)

    @TypeConverter
    fun itemStatusToString(value: ItemStatus): String = value.name

    @TypeConverter
    fun stringToItemStatus(value: String): ItemStatus = ItemStatus.valueOf(value)
}
