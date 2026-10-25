package echo.music.iad1tya.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconType(val value: Int) {
  DEFAULT(0),
  OBSIDIAN(1),
  CRIMSON(2),
  MIDNIGHT(3),
  PURPLE(4),
  DARK(5),
  LIGHT(6);

  companion object {
    fun fromValue(value: Int): AppIconType =
      entries.find { it.value == value } ?: DEFAULT
  }
}

object IconUtils {
  fun setIcon(context: Context, iconType: AppIconType) {
    val pm = context.packageManager

    val defaultAlias = ComponentName(context, "echo.music.iad1tya.MainActivityAlias")
    val obsidian = ComponentName(context, "echo.music.iad1tya.MainActivityObsidian")
    val crimson = ComponentName(context, "echo.music.iad1tya.MainActivityCrimson")
    val midnight = ComponentName(context, "echo.music.iad1tya.MainActivityMidnight")
    val purple = ComponentName(context, "echo.music.iad1tya.MainActivityPurple")
    val dark = ComponentName(context, "echo.music.iad1tya.MainActivityDark")
    val light = ComponentName(context, "echo.music.iad1tya.MainActivityLight")

    pm.setComponentEnabledSetting(
      defaultAlias,
      if (iconType == AppIconType.DEFAULT) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      obsidian,
      if (iconType == AppIconType.OBSIDIAN) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      crimson,
      if (iconType == AppIconType.CRIMSON) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      midnight,
      if (iconType == AppIconType.MIDNIGHT) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      purple,
      if (iconType == AppIconType.PURPLE) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      dark,
      if (iconType == AppIconType.DARK) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
    pm.setComponentEnabledSetting(
      light,
      if (iconType == AppIconType.LIGHT) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
      else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      PackageManager.DONT_KILL_APP
    )
  }
}
