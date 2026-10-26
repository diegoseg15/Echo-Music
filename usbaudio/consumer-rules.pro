# Keep classes and constructors looked up from jni_bridge.cpp.
-keep class com.vylune.music.usbaudio.model.DacFormat { <init>(...); }
-keep class com.vylune.music.usbaudio.model.DacCapabilities { <init>(...); }

# Keep names used by the exported JNI method symbols.
-keepclasseswithmembernames class com.vylune.music.usbaudio.UsbAudioDriver { native <methods>; }
