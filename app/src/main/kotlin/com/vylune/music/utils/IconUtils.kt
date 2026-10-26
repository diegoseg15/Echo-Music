package com.vylune.music.utils

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

    val defaultAlias = ComponentName(context, "com.vylune.music.MainActivityAlias")
    val obsidian = ComponentName(context, "com.vylune.music.MainActivityObsidian")
    val crimson = ComponentName(context, "com.vylune.music.MainActivityCrimson")
    val midnight = ComponentName(context, "com.vylune.music.MainActivityMidnight")
    val purple = ComponentName(context, "com.vylune.music.MainActivityPurple")
    val dark = ComponentName(context, "com.vylune.music.MainActivityDark")
    val light = ComponentName(context, "com.vylune.music.MainActivityLight")

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
