package app.simple.inure.crash;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import androidx.annotation.NonNull;
import app.simple.inure.activities.app.CrashReporterActivity;
import app.simple.inure.database.instances.StackTraceDatabase;
import app.simple.inure.models.StackTrace;
import app.simple.inure.preferences.CrashPreferences;

/*
 * Ref: https://stackoverflow.com/questions/601503/how-do-i-obtain-crash-data-from-my-android-application
 */
public class CrashReport implements Thread.UncaughtExceptionHandler {
    
    private final String TAG = getClass().getSimpleName();
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    
    public CrashReport(Context context) {
        this.context = context;
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    public void uncaughtException(@NonNull Thread thread, Throwable throwable) {
        final long crashTimeStamp = System.currentTimeMillis();
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
    
        throwable.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
    
        Utils.create(stacktrace, new File(context.getExternalFilesDir("logs"), "crashLog_" + crashTimeStamp));
        CrashPreferences.INSTANCE.saveCrashLog(crashTimeStamp);
        CrashPreferences.INSTANCE.saveMessage(throwable.toString());
        CrashPreferences.INSTANCE.saveCause(Utils.getCause(throwable).toString());
    
        Intent intent = new Intent(context, CrashReporterActivity.class);
        intent.putExtra(CrashReporterActivity.MODE_NORMAL, stacktrace);
        context.startActivity(intent);
    
        defaultUncaughtExceptionHandler.uncaughtException(thread, throwable);
    }
    
    public void initialize() {
        long timeStamp = CrashPreferences.INSTANCE.getCrashLog();
    
        try {
            if (timeStamp != CrashPreferences.CRASH_TIMESTAMP_EMPTY_DEFAULT) {
                String stack = Utils.read(new File(context.getExternalFilesDir("logs"), "crashLog_" + timeStamp));
                Intent intent = new Intent(context, CrashReporterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(CrashReporterActivity.MODE_NORMAL, stack);
                context.startActivity(intent);
            }
            
            Thread.setDefaultUncaughtExceptionHandler(new CrashReport(context));
        } catch (RuntimeException e) {
            if (context.getExternalFilesDir("logs").delete()) {
                Log.e(TAG, "Crash handler crashed -----> deleted crash logs");
            }
        }
    }
    
    @SuppressWarnings ("unused")
    public void saveTraceToDataBase(Throwable throwable) {
        new Thread(() -> {
            Log.d(TAG, "Thread started");
            StackTrace stackTrace = new StackTrace(throwable);
            StackTraceDatabase stackTraceDatabase = StackTraceDatabase.Companion.getInstance(context);
            stackTraceDatabase.stackTraceDao().insertTrace(stackTrace);
            Log.d(TAG, "Trace saved to database");
        }).start();
    }
}
