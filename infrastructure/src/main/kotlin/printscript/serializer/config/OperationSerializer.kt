package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.Operation

@Serializable
private data class OperationSurrogate(
    val op: String,
    val operands: List<String>,
    val result: String,
    val commutative: Boolean = true,
)

object OperationSerializer : KSerializer<Operation> {
    private val surrogateSerializer = OperationSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Operation,
    ) {
        val surrogate =
            OperationSurrogate(
                op = value.op,
                operands = value.operands,
                result = value.result,
                commutative = value.commutative,
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): Operation {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return Operation(
            op = surrogate.op,
            operands = surrogate.operands,
            result = surrogate.result,
            commutative = surrogate.commutative,
        )
    }
}
