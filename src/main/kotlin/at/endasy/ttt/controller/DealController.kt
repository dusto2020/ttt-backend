package at.endasy.ttt.controller

import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.security.requireAuth
import at.endasy.ttt.service.DealService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/deals")
class DealController(private val dealService: DealService) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User?): ResponseEntity<Any> {
        requireAuth(principal)?.let { return it }
        return ResponseEntity.ok(dealService.findActiveDeals())
    }
}
