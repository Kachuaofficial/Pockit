package com.invatech.pockit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.invatech.pockit.data.local.converters.RoomConverters
import com.invatech.pockit.data.local.dao.FriendDao
import com.invatech.pockit.data.local.dao.NotificationDao
import com.invatech.pockit.data.local.dao.SpaceDao
import com.invatech.pockit.data.local.dao.WalletDao
import com.invatech.pockit.data.local.entity.FriendEntity
import com.invatech.pockit.data.local.entity.FriendTransactionEntity
import com.invatech.pockit.data.local.entity.NotificationEntity
import com.invatech.pockit.data.local.entity.SpaceEntity
import com.invatech.pockit.data.local.entity.SpaceExpenseEntity
import com.invatech.pockit.data.local.entity.WalletTransactionEntity

@Database(
    entities = [
        SpaceEntity::class,
        SpaceExpenseEntity::class,
        WalletTransactionEntity::class,
        FriendEntity::class,
        FriendTransactionEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PockitDatabase : RoomDatabase() {

    abstract fun spaceDao(): SpaceDao
    abstract fun walletDao(): WalletDao
    abstract fun friendDao(): FriendDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: PockitDatabase? = null

        fun getDatabase(context: Context): PockitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PockitDatabase::class.java,
                    "pockit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
