package com.invatech.pockit

/*
 * Device instrumentation checks for supported Room migration paths and schema compatibility.
 */
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.invatech.pockit.data.local.database.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class DatabaseMigrationTest {
    @Test fun versionTwoUpgradeKeepsWalletEntries() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration_" + java.util.UUID.randomUUID().toString()
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val sqlite = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `spaces` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `themeIndex` INTEGER NOT NULL, `inviteCode` TEXT NOT NULL, `members` TEXT NOT NULL, `createdBy` TEXT NOT NULL, `type` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `space_expenses` (`id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `paidById` TEXT NOT NULL, `shares` TEXT NOT NULL, `dateLabel` TEXT NOT NULL, `isSettlement` INTEGER NOT NULL, `isConfirmed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `wallet_transactions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `amountPaise` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `friends` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `avatarEmoji` TEXT NOT NULL, `note` TEXT NOT NULL, `friendshipId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `friend_transactions` (`id` TEXT NOT NULL, `friendId` TEXT NOT NULL, `amount` REAL NOT NULL, `direction` TEXT NOT NULL, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT, `payerId` TEXT, `creatorId` TEXT NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))""")
        sqlite.execSQL("""CREATE TABLE IF NOT EXISTS `notifications` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `targetId` TEXT, PRIMARY KEY(`id`))""")

        sqlite.execSQL("INSERT INTO wallet_transactions (id,title,amountPaise,type,category,createdAtMillis) VALUES ('kept','Lunch',29,'EXPENSE','Food',123456)")
        sqlite.version = 2
        sqlite.close()
        val db = Room.databaseBuilder(context, PockitDatabase::class.java, name).addMigrations(PockitMigrations.FROM_2_TO_3, PockitMigrations.FROM_3_TO_4).build()
        try {
            assertEquals(29L, db.walletDao().getTransactionById("kept")?.amountPaise)
            assertTrue(db.budgetDao().getAllBudgets().isEmpty())
            assertTrue(db.pendingWalletDao().all().isEmpty())
            db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM subscriptions").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)) }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
