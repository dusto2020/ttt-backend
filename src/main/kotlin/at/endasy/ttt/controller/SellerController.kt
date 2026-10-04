package at.endasy.ttt.controller

import at.endasy.ttt.dto.toResponse
import at.endasy.ttt.security.TttOAuth2User
import at.endasy.ttt.service.SellerService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sellers")
class SellerController(private val sellerService: SellerService) {

    @GetMapping
    fun list(@AuthenticationPrincipal principal: TttOAuth2User): ResponseEntity<Any> {
        return ResponseEntity.ok(sellerService.findActive().map { it.toResponse() })
    }
}
