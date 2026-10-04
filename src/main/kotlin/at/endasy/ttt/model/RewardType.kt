package at.endasy.ttt.model

import com.fasterxml.jackson.annotation.JsonProperty

enum class RewardType(val wireName: String) {
    @JsonProperty("temu_credit") TEMU_CREDIT("temu_credit"),
    @JsonProperty("paypal_cashback") PAYPAL_CASHBACK("paypal_cashback"),
}
