#!/system/bin/sh
export CLASSPATH=/system/framework/supervision.jar
exec app_process /system/bin com.android.commands.supervision.Supervision "$@"
