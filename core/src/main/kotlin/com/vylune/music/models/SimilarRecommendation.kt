package com.vylune.music.models

import com.music.innertube.models.YTItem
import com.vylune.music.db.entities.LocalItem

data class SimilarRecommendation(
  val title: LocalItem,
  val items: List<YTItem>,
)
