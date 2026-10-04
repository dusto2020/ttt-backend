package at.endasy.ttt.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class EventType(val wireName: String) {
    @JsonProperty("claimcredit") CLAIMCREDIT("claimcredit"),
    @JsonProperty("wincredit") WINCREDIT("wincredit"),
}
