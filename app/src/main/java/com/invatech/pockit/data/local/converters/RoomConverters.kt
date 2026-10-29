package com.invatech.pockit.data.local.converters

import androidx.room.TypeConverter
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.TransactionStatus
import com.invatech.pockit.ui.home.model.WalletTransactionType
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.SpaceType
import org.json.JSONArray
import org.json.JSONObject

class RoomConverters {

    @TypeConverter
    fun fromMemberList(members: List<Member>?): String {
        if (members == null) return "[]"
        val array = JSONArray()
        members.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toMemberList(json: String?): List<Member> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Member>()
        return try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Member(
                        id = obj.getString("id"),
                        name = obj.getString("name")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromSharesMap(shares: Map<String, Double>?): String {
        if (shares == null) return "{}"
        val obj = JSONObject()
        shares.forEach { (k, v) ->
            obj.put(k, v)
        }
        return obj.toString()
    }

    @TypeConverter
    fun toSharesMap(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, Double>()
        return try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getDouble(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromSpaceType(type: SpaceType?): String = type?.name ?: SpaceType.BILL_SPLIT.name

    @TypeConverter
    fun toSpaceType(name: String?): SpaceType = runCatching {
        if (name != null) SpaceType.valueOf(name) else SpaceType.BILL_SPLIT
    }.getOrDefault(SpaceType.BILL_SPLIT)

    @TypeConverter
    fun fromWalletTransactionType(type: WalletTransactionType?): String =
        type?.name ?: WalletTransactionType.EXPENSE.name

    @TypeConverter
    fun toWalletTransactionType(name: String?): WalletTransactionType = runCatching {
        if (name != null) WalletTransactionType.valueOf(name) else WalletTransactionType.EXPENSE
    }.getOrDefault(WalletTransactionType.EXPENSE)

    @TypeConverter
    fun fromExpenseDirection(direction: ExpenseDirection?): String =
        direction?.name ?: ExpenseDirection.GAVE_UDHAR.name

    @TypeConverter
    fun toExpenseDirection(name: String?): ExpenseDirection = runCatching {
        if (name != null) ExpenseDirection.valueOf(name) else ExpenseDirection.GAVE_UDHAR
    }.getOrDefault(ExpenseDirection.GAVE_UDHAR)

    @TypeConverter
    fun fromTransactionStatus(status: TransactionStatus?): String =
        status?.name ?: TransactionStatus.CONFIRMED.name

    @TypeConverter
    fun toTransactionStatus(name: String?): TransactionStatus = runCatching {
        if (name != null) TransactionStatus.valueOf(name) else TransactionStatus.CONFIRMED
    }.getOrDefault(TransactionStatus.CONFIRMED)
}
