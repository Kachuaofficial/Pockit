package com.invatech.pockit;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import java.lang.reflect.Constructor;

/** Framework-only runner: no dependency on Kotlin/AndroidX symbols renamed in the target APK. */
public final class ReleaseSmokeInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            ClassLoader loader = getTargetContext().getClassLoader();
            Class<?> finance = loader.loadClass("com.invatech.pockit.data.local.database.FinanceRecord");
            Object row = finance.getConstructor(String.class, String.class, String.class, String.class,
                    long.class, String.class, String.class, String.class, String.class, long.class)
                    .newInstance("id", "owner", "UDHAR", "Friend", 125L, "LENT", "Other", "", "SAVED", 1234L);
            if (!"Friend".equals(finance.getMethod("getName").invoke(row)) ||
                    !Long.valueOf(125L).equals(finance.getMethod("getAmountPaise").invoke(row))) {
                throw new AssertionError("Finance reflection values changed");
            }
            String[][] models = {
                {"CategoryBudgetEntity", "getCategory", "getMonthlyLimitPaise"},
                {"SubscriptionEntity", "getTitle", "getAmountPaise", "isActive"},
                {"NotificationEntity", "getTitle", "getTimestamp", "isRead"}
            };
            for (String[] model : models) {
                Class<?> type = loader.loadClass("com.invatech.pockit.data.local.entity." + model[0]);
                for (int i = 1; i < model.length; i++) type.getMethod(model[i]);
            }
            for (String name : new String[]{"PockitDatabase_Impl", "FinanceDatabase_Impl"}) {
                loader.loadClass("com.invatech.pockit.data.local.database." + name).getConstructor().newInstance();
            }
            for (String name : new String[]{"PockitEngagementWorker", "SubscriptionReminderWorker"}) {
                Class<?> type = loader.loadClass("com.invatech.pockit.data.notification." + name);
                boolean found = false;
                for (Constructor<?> constructor : type.getConstructors()) {
                    Class<?>[] args = constructor.getParameterTypes();
                    if (args.length == 2 && args[0].getName().equals("android.content.Context") &&
                            args[1].getName().equals("androidx.work.WorkerParameters")) found = true;
                }
                if (!found) throw new AssertionError("Worker constructor missing: " + name);
            }
            result.putString("stream", "PASS: minified Firestore models, Room implementations and worker constructors\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
