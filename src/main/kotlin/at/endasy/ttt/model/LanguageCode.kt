package at.endasy.ttt.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class LanguageCode(val wireName: String) {
    @JsonProperty("DE") DE("DE"),
    @JsonProperty("EN") EN("EN"),
    @JsonProperty("JP") JP("JP"),
}
