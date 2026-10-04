package at.endasy.ttt.dto

import java.math.BigDecimal

data class PortfolioDto(
    val totalResaleAndPaypalIn: BigDecimal,
    val totalCashPaid: BigDecimal,
    val netCashPosition: BigDecimal,
    val totalCreditReturned: BigDecimal,
    val totalCreditUsed: BigDecimal,
    val activeCreditFloat: BigDecimal,
    val isFreeRoll: Boolean,
)
