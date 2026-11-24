package com.invatech.pockit.data.local.database

/*
 * Separate Room database for owner/kind-scoped finance records.
 * The same row type stores udhar, goals and pending UPI items, so direction/status must be interpreted with kind.
 */

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "finance_records", indices = [Index(value = ["owner", "kind"])])
data class FinanceRecord(
    @PrimaryKey val id: String,
    val owner: String,
    val kind: String,
    val name: String,
    val amountPaise: Long,
    val direction: String,
    val category: String = "Other",
    val note: String = "",
    val status: String = "PENDING",
    val occurredAt: Long = System.currentTimeMillis()
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_records WHERE owner = :owner AND kind = :kind ORDER BY occurredAt DESC")
    fun observe(owner: String, kind: String): Flow<List<FinanceRecord>>
    @Query("SELECT * FROM finance_records WHERE owner = :owner AND kind = :kind ORDER BY occurredAt DESC")
    suspend fun list(owner: String, kind: String): List<FinanceRecord>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: FinanceRecord): Long
    @Update suspend fun update(record: FinanceRecord)
    @Query("UPDATE finance_records SET status = :status WHERE id = :id AND owner = :owner")
    suspend fun status(id: String, owner: String, status: String)
    @Query("DELETE FROM finance_records WHERE id = :id AND owner = :owner")
    suspend fun delete(id: String, owner: String)
}

@Database(entities = [FinanceRecord::class], version = 1, exportSchema = true)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun records(): FinanceDao
    companion object {
        @Volatile private var instance: FinanceDatabase? = null
        fun get(context: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, FinanceDatabase::class.java, "pockit_finance")
                .build().also { instance = it }
        }
    }
}
