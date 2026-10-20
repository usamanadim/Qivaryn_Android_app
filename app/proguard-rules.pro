# Qivaryn release rules.
#
# Minification is disabled in app/build.gradle. If it is enabled later, keep
# Chaquopy's Python bridge classes and their public methods reachable.
-keep class com.chaquo.python.** { *; }
-keep class com.qivaryn.app.engine.QivarynEngineBridge { *; }
