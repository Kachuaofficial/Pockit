package com.invatech.pockit.data.local.database

/*
 * Room database and DAO access for the main local cache, currently schema version 3.
 * Chooses a UID-derived database filename and preserves the recorded owner of the pre-account-scoped legacy cache.
 */

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.invatech.pockit.data.local.converters.RoomConverters
import com.invatech.pockit.data.local.dao.BudgetDao
import com.invatech.pockit.data.local.dao.FriendDao
import com.invatech.pockit.data.local.dao.NotificationDao
import com.invatech.pockit.data.local.dao.SpaceDao
import com.invatech.pockit.data.local.dao.SubscriptionDao
import com.invatech.pockit.data.local.dao.WalletDao
import com.invatech.pockit.data.local.entity.CategoryBudgetEntity
import com.invatech.pockit.data.local.entity.FriendEntity
import com.invatech.pockit.data.local.entity.FriendTransactionEntity
import com.invatech.pockit.data.local.entity.NotificationEntity
import com.invatech.pockit.data.local.entity.SpaceEntity
import com.invatech.pockit.data.local.entity.SpaceExpenseEntity
import com.invatech.pockit.data.local.entity.SubscriptionEntity
import com.invatech.pockit.data.local.entity.WalletTransactionEntity

@Database(
    entities = [
        SpaceEntity::class,
        SpaceExpenseEntity::class,
        WalletTransactionEntity::class,
        FriendEntity::class,
        FriendTransactionEntity::class,
        NotificationEntity::class,
        SubscriptionEntity::class,
        CategoryBudgetEntity::class,
        com.invatech.pockit.data.local.entity.PendingWalletEntry::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class PockitDatabase : RoomDatabase() {

    abstract fun spaceDao(): SpaceDao
    abstract fun walletDao(): WalletDao
    abstract fun friendDao(): FriendDao
    abstract fun notificationDao(): NotificationDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun pendingWalletDao(): com.invatech.pockit.data.local.dao.PendingWalletDao

    companion object {
        private val instances = mutableMapOf<String, PockitDatabase>()

        /**
         * Returns the current account cache instance. A SHA-256 UID suffix separates database
         * filenames; it is a naming choice, not database encryption. Only the recorded legacy
         * owner continues using the original database filename.
         */
        fun getDatabase(context: Context): PockitDatabase {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "signed_out"
            val prefs = context.getSharedPreferences("pockit_database_owner", 0)
            return synchronized(this) {
                var legacyOwner = prefs.getString("legacy_owner", null)
                val suffix = java.security.MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
                val name = if (legacyOwner == uid) "pockit_database" else "pockit_$suffix"
                instances[name]?.let { return@synchronized it }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PockitDatabase::class.java,
                    name
                )
                    .addMigrations(PockitMigrations.FROM_1_TO_2, PockitMigrations.FROM_2_TO_3, PockitMigrations.FROM_3_TO_4)
                    .build()
                instances[name] = instance
                instance
            }
        }
    }
}
