package at.endasy.ttt.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class CountryCode(val wireName: String) {
    @JsonProperty("DE") DE("DE"),
    @JsonProperty("AT") AT("AT"),
}
