package printscript.evaluator

enum class MatchType {
    VALID,
    INVALID,
    PARTIAL,
    ;

    companion object {
        /**
         * Returns the appropriate MatchType based on the provided exact and partial match conditions.
         *
         * @param exact A function that returns true if the exact match condition is met.
         * @param partial A function that returns true if the partial match condition is met.
         * @return The corresponding MatchType (VALID, PARTIAL, or INVALID).
         */
        fun of(
            exact: () -> Boolean,
            partial: () -> Boolean,
        ): MatchType =
            when {
                exact() -> VALID
                partial() -> PARTIAL
                else -> INVALID
            }
    }
}
