package printscript.domain

data class FormatterLanguageConfig(
    val rules: List<LanguageFormatRuleSpec> = emptyList(),
    val userBindings: List<UserRuleBinding> = emptyList(),
) {
    init {
        validate()
    }

    private fun validate() {
        rules.forEach { rule ->
            require(rule.token.isNotBlank()) { "Language format rule '${rule.type}' needs a token" }
        }

        val userTypes = userBindings.map { it.userType }
        val duplicates =
            userTypes
                .groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .keys
        require(duplicates.isEmpty()) { "Duplicate userType in formatter language config: $duplicates" }

        userBindings.forEach { binding ->
            require(binding.userType.isNotBlank()) { "userType must not be blank" }
            require(binding.token.isNotBlank()) { "Binding '${binding.userType}' needs a token" }
            require(binding.param == UserRuleBinding.PARAM_ENABLED || binding.param == UserRuleBinding.PARAM_COUNT) {
                "Binding '${binding.userType}' param must be 'enabled' or 'count'"
            }
        }
    }
}

data class LanguageFormatRuleSpec(
    val type: String,
    val token: String,
    val value: String? = null,
    val previous: String? = null,
)

data class UserRuleBinding(
    val userType: String,
    val type: String,
    val token: String,
    val value: String? = null,
    val previous: String? = null,
    val param: String,
    val defaultEnabled: Boolean? = null,
    val defaultCount: Int? = null,
) {
    companion object {
        const val PARAM_ENABLED = "enabled"
        const val PARAM_COUNT = "count"
    }
}
