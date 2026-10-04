package at.endasy.ttt.dto

import at.endasy.ttt.model.CountryCode
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemuImportRequest(
    @JsonProperty("goods_info")
    val goodsInfo: TemuGoodsInfo,
    @JsonProperty("short_link")
    val shortLink: String,
    @JsonProperty("product_url")
    val productUrl: String? = null, // <-- NEU
    @JsonProperty("country_code")
    val countryCode: CountryCode? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemuGoodsInfo(
    @JsonProperty("goods_id")
    val goodsId: Long,
    @JsonProperty("mall_id")
    val mallId: Long,
    val title: String,
    @JsonProperty("price_info")
    val priceInfo: TemuPriceInfo,
    @JsonProperty("thumb_url")
    val thumbUrl: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemuPriceInfo(
    val price: Long, // in Cent, z. B. 23310 = 233.10 €
    val currency: String = "EUR",
)