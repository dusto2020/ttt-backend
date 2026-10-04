package at.endasy.ttt.security

import org.springframework.http.ResponseEntity

/**
 * Returns a 401/403 [ResponseEntity] if the principal is missing or not an admin, `null` otherwise.
 */
fun requireAdmin(principal: TttOAuth2User?): ResponseEntity<Any>? {
    if (principal == null) return ResponseEntity.status(401).build()
    if (!principal.isAdmin) return ResponseEntity.status(403).build()
    return null
}

/**
 * Returns a 401 [ResponseEntity] if the principal is missing, `null` otherwise.
 */
fun requireAuth(principal: TttOAuth2User?): ResponseEntity<Any>? {
    if (principal == null) return ResponseEntity.status(401).build()
    return null
}
