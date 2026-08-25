package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.NodeConfig
import printscript.domain.Operation
import printscript.domain.TypeSystemConfig

@Serializable
private data class TypeSystemConfigSurrogate(
    val types: List<String>,
    val literals: Map<String, String> = emptyMap(),
    val operations: List<
        @Serializable(with = OperationSerializer::class)
        Operation,
    > = emptyList(),
    val nodes: Map<
        String,
        @Serializable(with = NodeConfigSerializer::class)
        NodeConfig,
    > = emptyMap(),
)

object TypeSystemConfigSerializer : KSerializer<TypeSystemConfig> {
    private val surrogateSerializer = TypeSystemConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: TypeSystemConfig,
    ) {
        val surrogate =
            TypeSystemConfigSurrogate(
                types = value.types,
                literals = value.literals,
                operations = value.operations,
                nodes = value.nodes,
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): TypeSystemConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return TypeSystemConfig(
            types = surrogate.types,
            literals = surrogate.literals,
            operations = surrogate.operations,
            nodes = surrogate.nodes,
        )
    }
}
