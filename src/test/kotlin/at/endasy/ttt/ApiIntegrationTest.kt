package at.endasy.ttt

import at.endasy.ttt.security.TttOAuth2User
import com.teamrestocks.ttt.jooq.tables.references.USERS
import java.math.BigDecimal
import java.util.UUID
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * End-to-end happy path integration test exercising the full stack (jOOQ + Flyway + PostgreSQL)
 * via MockMvc, bypassing the real Discord OAuth2 flow by injecting a [TttOAuth2User] principal
 * directly into the security context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var dsl: DSLContext

    private lateinit var adminUserId: UUID

    @BeforeEach
    fun setUp() {
        val record = dsl.insertInto(USERS)
            .set(USERS.DISCORD_ID, "discord-${UUID.randomUUID()}")
            .set(USERS.DISCORD_USERNAME, "TestAdmin")
            .set(USERS.IS_ADMIN, true)
            .returning(USERS.ID)
            .fetchOne()!!
        adminUserId = record.id!!
    }

    private fun asAdmin(): RequestPostProcessor {
        val principal = TttOAuth2User(
            delegate = DefaultOAuth2User(listOf(SimpleGrantedAuthority("ROLE_USER")), mapOf("id" to adminUserId.toString()), "id"),
            userId = adminUserId,
            discordId = "discord-admin",
            discordUsername = "TestAdmin",
            avatarUrl = null,
            isAdmin = true,
        )
        return SecurityMockMvcRequestPostProcessors.oauth2Login().oauth2User(principal)
    }

    @Test
    fun `happy path - deal feed, device, order and portfolio`() {
        // 1. Create a seller as admin
        val sellerJson = mockMvc.perform(
            post("/api/admin/sellers")
                .with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to "Verified Seller",
                            "storeUrl" to "https://temu.com/seller",
                            "isActive" to true,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val sellerId = objectMapper.readTree(sellerJson).get("id").asText()

        // 2. Create a product as admin: temuPrice=20, cardmarketPrice=30 -> spreadRatio = 1.5
        val productJson = mockMvc.perform(
            post("/api/admin/products")
                .with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "name" to "Booster Box",
                            "languageCode" to "EN",
                            "temuAffiliateUrl" to "https://temu.com/product",
                            "cardmarketUrl" to "https://cardmarket.com/product",
                            "temuPrice" to 20.00,
                            "cardmarketPrice" to 30.00,
                            "isActive" to true,
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val productId = objectMapper.readTree(productJson).get("id").asText()

        // 3. GET /api/deals and assert spreadRatio is calculated correctly (30 / 20 = 1.5)
        mockMvc.perform(get("/api/deals").with(asAdmin()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id=='$productId')].spreadRatio").value(1.5))

        // 4. Create a user device
        val deviceJson = mockMvc.perform(
            post("/api/devices")
                .with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("label" to "Phone 1 (Main)"))),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val deviceId = objectMapper.readTree(deviceJson).get("id").asText()

        // 5. Log an order: cashPaid=30, creditUsed=70, totalReturn=90
        mockMvc.perform(
            post("/api/orders")
                .with(asAdmin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "deviceId" to deviceId,
                            "sellerId" to sellerId,
                            "orderSn" to "SN-12345",
                            "eventType" to "claimcredit",
                            "rewardType" to "temu_credit",
                            "cashPaid" to 30.00,
                            "creditUsed" to 70.00,
                            "totalReturn" to 90.00,
                            "items" to listOf(mapOf("productId" to productId, "quantity" to 1)),
                        ),
                    ),
                ),
        )
            .andExpect(status().isOk)

        // 6. GET /api/portfolio and assert netCashPosition == -30.00 and isFreeRoll == false
        mockMvc.perform(get("/api/portfolio").with(asAdmin()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.netCashPosition").value(BigDecimal("-30.00").toDouble()))
            .andExpect(jsonPath("$.isFreeRoll").value(false))
    }
}
