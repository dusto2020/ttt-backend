package at.endasy.ttt.service

import at.endasy.ttt.dto.PortfolioDto
import com.teamrestocks.ttt.jooq.tables.references.ORDERS
import com.teamrestocks.ttt.jooq.tables.references.ORDER_ITEMS
import java.math.BigDecimal
import java.util.UUID
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Service

@Service
class PortfolioService(private val dsl: DSLContext) {

    /**
     * Computes the fund-accounting treasury snapshot for a user (or globally, for admins).
     * - Net real cash position: (total PayPal cash + total realized resales) − total out-of-pocket cash paid.
     * - Active credit float: total credit returned (claimed) − total credit used.
     * - Free-roll status: net cash position >= 0.
     */
    fun computePortfolio(userId: UUID, isAdmin: Boolean): PortfolioDto {
        val condition = if (isAdmin) DSL.trueCondition() else ORDERS.USER_ID.eq(userId)

        // 1. Echtes Geld ausgegeben:
        val totalCashPaid = dsl.select(DSL.coalesce(DSL.sum(ORDERS.CASH_PAID), BigDecimal.ZERO))
            .from(ORDERS)
            .where(condition)
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO

        // 2. Echtes PayPal-Cashback erhalten:
        val totalPaypalIn = dsl.select(DSL.coalesce(DSL.sum(ORDERS.CLAIMED_AMOUNT), BigDecimal.ZERO))
            .from(ORDERS)
            .where(condition)
            .and(ORDERS.REWARD_TYPE.eq("paypal_cashback"))
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO

        // 3. NEU: Realisierte Verkaufserlöse aus dem Inventar (Status = SOLD):
        val totalResales = dsl.select(DSL.coalesce(DSL.sum(ORDER_ITEMS.RESALE_PRICE), BigDecimal.ZERO))
            .from(ORDER_ITEMS)
            .join(ORDERS).on(ORDER_ITEMS.ORDER_ID.eq(ORDERS.ID))
            .where(condition)
            .and(ORDER_ITEMS.STATUS.eq("SOLD"))
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO

        // Gesamtes reales Geld rein = PayPal Cashback + Verkäufe
        val totalResaleAndPaypalIn = totalPaypalIn.add(totalResales)

        // 4. Temu Guthaben-Tracking:
        val totalCreditUsed = dsl.select(DSL.coalesce(DSL.sum(ORDERS.CREDIT_USED), BigDecimal.ZERO))
            .from(ORDERS)
            .where(condition)
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO

        val totalCreditReturned = dsl.select(DSL.coalesce(DSL.sum(ORDERS.CLAIMED_AMOUNT), BigDecimal.ZERO))
            .from(ORDERS)
            .where(condition)
            .and(ORDERS.REWARD_TYPE.eq("temu_credit"))
            .fetchOne(0, BigDecimal::class.java) ?: BigDecimal.ZERO

        val netCashPosition = totalResaleAndPaypalIn.subtract(totalCashPaid)
        val activeCreditFloat = totalCreditReturned.subtract(totalCreditUsed)

        return PortfolioDto(
            totalResaleAndPaypalIn = totalResaleAndPaypalIn,
            totalCashPaid = totalCashPaid,
            netCashPosition = netCashPosition,
            totalCreditReturned = totalCreditReturned,
            totalCreditUsed = totalCreditUsed,
            activeCreditFloat = activeCreditFloat,
            isFreeRoll = netCashPosition.signum() >= 0,
        )
    }
}