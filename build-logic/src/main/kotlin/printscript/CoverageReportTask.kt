package printscript

import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

abstract class CoverageReportTask : DefaultTask() {

    @get:Input
    abstract val moduleReports: MapProperty<String, String> // módulo -> ruta absoluta a report.xml

    private data class Metric(val covered: Int, val missed: Int) {
        val total get() = covered + missed
        val pct get() = if (total == 0) 0.0 else covered.toDouble() / total * 100
    }

    private data class ModuleCoverage(
        val name: String,
        val line: Metric,
        val method: Metric,
        val branch: Metric,
    )

    @TaskAction
    fun report() {
        val results = mutableListOf<ModuleCoverage>()

        moduleReports.get().toSortedMap().forEach { (moduleName, path) ->
            val file = File(path)
            if (!file.exists()) {
                logger.lifecycle("⚠️  $moduleName: sin reporte (¿corrió koverXmlReport?)")
                return@forEach
            }

            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val counters = doc.getElementsByTagName("counter")

            var line = Metric(0, 0)
            var method = Metric(0, 0)
            var branch = Metric(0, 0)

            for (i in 0 until counters.length) {
                val node = counters.item(i)
                val attrs = node.attributes
                val type = attrs.getNamedItem("type")?.nodeValue ?: continue
                val covered = attrs.getNamedItem("covered")?.nodeValue?.toIntOrNull() ?: 0
                val missed = attrs.getNamedItem("missed")?.nodeValue?.toIntOrNull() ?: 0

                // El XML tipo JaCoCo trae counters anidados a distintos niveles
                // (package, class, method...); nos quedamos con los del nivel
                // más alto (report/module), que son los que reflejan el total.
                if (node.parentNode?.nodeName == "report") {
                    when (type) {
                        "LINE" -> line = Metric(covered, missed)
                        "METHOD" -> method = Metric(covered, missed)
                        "BRANCH" -> branch = Metric(covered, missed)
                    }
                }
            }

            results += ModuleCoverage(moduleName, line, method, branch)
        }

        printTable(results)
    }

    private fun printTable(results: List<ModuleCoverage>) {
        val header = String.format(
            "%-16s %10s %10s %10s",
            "Módulo", "Línea", "Método", "Branch",
        )
        val separator = "─".repeat(header.length)

        println()
        println("📊  Coverage por módulo")
        println(separator)
        println(header)
        println(separator)

        results.forEach { m ->
            println(
                String.format(
                    "%-16s %9.2f%% %9.2f%% %9.2f%%",
                    m.name,
                    m.line.pct,
                    m.method.pct,
                    m.branch.pct,
                ),
            )
        }

        println(separator)

        val totalLine = Metric(results.sumOf { it.line.covered }, results.sumOf { it.line.missed })
        val totalMethod = Metric(results.sumOf { it.method.covered }, results.sumOf { it.method.missed })
        val totalBranch = Metric(results.sumOf { it.branch.covered }, results.sumOf { it.branch.missed })

        println(
            String.format(
                "%-16s %9.2f%% %9.2f%% %9.2f%%",
                "TOTAL",
                totalLine.pct,
                totalMethod.pct,
                totalBranch.pct,
            ),
        )
        println(separator)
        println(
            "Líneas: ${totalLine.covered}/${totalLine.total}   " +
                "Métodos: ${totalMethod.covered}/${totalMethod.total}   " +
                "Branches: ${totalBranch.covered}/${totalBranch.total}",
        )
        println()
    }
}
