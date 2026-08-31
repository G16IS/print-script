package printscript.formatter

/**
 * Language-level knobs for the formatter pipeline. Change these here
 * (they are not user YAML).
 */
object FormatterConfig {
    /**
     * User style file, relative to the working directory / project root.
     * The CLI / use-case will read this path; override later if the convention changes.
     */
    const val USER_YAML_PATH = ".printscript/formatter.yml"

    const val LANGUAGE_JSON_RESOURCE = "formatter-language.json"
}
