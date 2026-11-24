package com.invatech.pockit.data.local.database

/*
 * Explicit Room upgrade paths from version 1 to 2 and 2 to 3.
 * Creates subscription/budget tables without destructive recreation of the existing ledger tables.
 */

object PockitMigrations {
    val FROM_1_TO_2 = object : androidx.room.migration.Migration(1, 2) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amountPaise INTEGER NOT NULL, billingCycle TEXT NOT NULL, dueDayOfMonth INTEGER NOT NULL, category TEXT NOT NULL, reminderDaysBefore INTEGER NOT NULL, paymentMethod TEXT NOT NULL, notes TEXT NOT NULL, isActive INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, nextDueMillis INTEGER NOT NULL)")
                        }
                    }
    val FROM_2_TO_3 = object : androidx.room.migration.Migration(2, 3) {
                        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amountPaise INTEGER NOT NULL, billingCycle TEXT NOT NULL, dueDayOfMonth INTEGER NOT NULL, category TEXT NOT NULL, reminderDaysBefore INTEGER NOT NULL, paymentMethod TEXT NOT NULL, notes TEXT NOT NULL, isActive INTEGER NOT NULL, createdAtMillis INTEGER NOT NULL, nextDueMillis INTEGER NOT NULL)")
                            db.execSQL("CREATE TABLE IF NOT EXISTS category_budgets (category TEXT NOT NULL PRIMARY KEY, monthlyLimitPaise INTEGER NOT NULL, warningThresholdPercent INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL)")
                        }
                    }
    val FROM_3_TO_4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS pending_wallet_entries (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, amountPaise INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, createdAtMillis INTEGER NOT NULL)")
        }
    }
}
