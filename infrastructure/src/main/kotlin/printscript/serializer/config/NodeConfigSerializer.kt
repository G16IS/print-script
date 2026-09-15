package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.NodeConfig

@Serializable
private data class NodeConfigSurrogate(
    val kind: String,
    val id: String? = null,
    val declaredType: String? = null,
    val expression: String? = null,
    val callee: String? = null,
    val args: List<String> = emptyList(),
    val mutable: Boolean = true,
    val then: String? = null,
    @SerialName("else")
    val elseClause: String? = null,
    val block: String? = null,
    val returnTypes: Map<String, String> = emptyMap(),
)

object NodeConfigSerializer : KSerializer<NodeConfig> {
    private val surrogateSerializer = NodeConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: NodeConfig,
    ) {
        val surrogate =
            NodeConfigSurrogate(
                kind = value.kind,
                id = value.id,
                declaredType = value.declaredType,
                expression = value.expression,
                callee = value.callee,
                args = value.args,
                mutable = value.mutable,
                then = value.then,
                elseClause = value.elseClause,
                block = value.block,
                returnTypes = value.returnTypes,
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): NodeConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return NodeConfig(
            kind = surrogate.kind,
            id = surrogate.id,
            declaredType = surrogate.declaredType,
            expression = surrogate.expression,
            callee = surrogate.callee,
            args = surrogate.args,
            mutable = surrogate.mutable,
            then = surrogate.then,
            elseClause = surrogate.elseClause,
            block = surrogate.block,
            returnTypes = surrogate.returnTypes,
        )
    }
}
