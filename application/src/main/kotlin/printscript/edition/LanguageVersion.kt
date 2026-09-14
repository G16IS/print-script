package printscript.edition

import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.util.Result

data class LanguageVersion(
    val major: Int,
    val minor: Int,
) {
    companion object {
        fun parse(raw: String): Result<LanguageVersion, RuntimeError> {
            val version =
                parseDotted(raw) ?: return Result
                    .Err(LanguageVersionNotFound(raw))

            return Result.Ok(version)
        }

        private fun parseDotted(raw: String): LanguageVersion? {
            val parts = raw.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull()
            val minor = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size != 2 || major == null || minor == null) {
                return null
            }
            return if (parts[0] == major.toString() && parts[1] == minor.toString()) {
                LanguageVersion(major, minor)
            } else {
                null
            }
        }
    }
}
