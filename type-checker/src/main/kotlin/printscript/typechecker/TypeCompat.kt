package printscript.typechecker

import printscript.domain.NodeConfig

/**
 * Compatibilidad entre el tipo esperado y el resuelto.
 *
 * `readInput` / `readEnv` no tienen un tipo de retorno fijo: se adaptan al
 * destino. El type-system config los declara con [NodeConfig.ANY_TYPE], que
 * acá se trata como compatible con cualquier tipo.
 */
internal object TypeCompat {
    fun compatible(
        expected: String,
        actual: String,
    ): Boolean = actual == NodeConfig.ANY_TYPE || expected == actual
}
