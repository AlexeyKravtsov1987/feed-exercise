package com.lightricks.feedexercise.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


/*
*
      "configuration": "star-of-the-party.json",
      "id": "01E6GHMF4D6WN441KKNB81ZK0B",
      "isNew": true,
      "isPremium": false,
      "templateCategories": [
        "01DJ4TM161PHH15365HRTXH2GJ"
      ],
      "templateName": "slide-reveal-boundingbox-template.json",
      "templateThumbnailURI": "star-of-the-party-thumbnail.jpg"

* */
@JsonClass(generateAdapter = true)
data class TemplateMetadata(@field:Json(name = "id") val id: String,
                            @field:Json(name = "configuration") val configuration: String,
                            @field:Json(name = "isNew") val isNew: Boolean,
                            @field:Json(name = "isPremium") val isPremium: Boolean,
                            @field:Json(name = "templateCategories") val templateCategories: List<String>,
                            @field:Json(name = "templateName") val templateName: String,
                            @field:Json(name = "templateThumbnailURI") val templateThumbnailURI: String)
@JsonClass(generateAdapter = true)
data class TemplatesMetadata(@field:Json(name = "templatesMetadata") val templatesMetadata: List<TemplateMetadata>)