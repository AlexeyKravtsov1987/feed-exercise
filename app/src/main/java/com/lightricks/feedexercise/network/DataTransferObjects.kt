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
data class TemplateMetadataItem(val id: String,
                            val configuration: String,
                            val isNew: Boolean,
                            val isPremium: Boolean,
                            val templateCategories: List<String>,
                            val templateName: String,
                            val templateThumbnailURI: String)

@JsonClass(generateAdapter = true)
data class TemplatesMetadata(val templatesMetadata: List<TemplateMetadataItem>)