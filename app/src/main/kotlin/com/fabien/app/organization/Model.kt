package com.fabien.app.organization

import arrow.core.Either
import com.fabien.app.OrganizationCreationErrorList
import com.fabien.app.OrganizationDBError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

data class NewOrganization(
    val name: String,
    val nationalId: String,
    val zipCode: String?,
    val country: String,
    val city: String?,
    val address: String?,
    val active: Boolean,
)

@Serializable
data class Organization(
    @Serializable(with = UUIDdSerializer::class) val id: UUID,
    val name: String,
    val nationalId: String,
    val country: String,
    val zipCode: String?,
    val city: String?,
    val address: String?,
    val active: Boolean,
)

object UUIDdSerializer : KSerializer<UUID> {
    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value = value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID = decoder.decodeString().let(UUID::fromString)

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
}

data class AddOrganizationCommand(
    val name: String?,
    val nationalId: String?,
    val zipCode: String?,
    val country: String?,
    val city: String?,
    val address: String?,
    val active: Boolean?,
)

fun interface AddOrganizationCommandHandler {
    operator fun invoke(command: AddOrganizationCommand): Either<OrganizationCreationErrorList, Organization>
}

interface OrganizationRepository {
    suspend fun save(organization: NewOrganization): Either<OrganizationDBError, Organization>

    suspend fun get(id: UUID): Either<OrganizationDBError, Organization?>
}
