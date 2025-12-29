package im.bigs.pg.external.pg

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * TestPG API용 AES-256-GCM 암호화 유틸리티.
 * - 키 생성: SHA-256(API-KEY) → 32바이트
 * - IV: 12바이트 (96비트), Base64URL 디코딩
 * - 태그 길이: 128비트 (16바이트)
 * - 결과 인코딩: Base64URL(ciphertext || tag), 패딩 없음
 */
object TestPgEncryptor {
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(plaintext: String, apiKey: String, ivBase64: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(apiKey.toByteArray(Charsets.UTF_8))

        val ivBytes = Base64.getUrlDecoder().decode(ivBase64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext)
    }
}
