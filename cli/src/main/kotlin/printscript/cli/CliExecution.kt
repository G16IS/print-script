package printscript.cli

data class CliExecution(
    val statusCode: Int,
    val stdout: String,
    val stderr: String,
)
