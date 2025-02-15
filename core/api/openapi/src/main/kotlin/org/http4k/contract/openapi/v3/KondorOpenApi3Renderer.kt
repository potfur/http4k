package org.http4k.contract.openapi.v3

import com.ubertob.kondor.json.jsonnode.JsonNode
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.JsonSchemaCreator
import org.http4k.contract.jsonschema.v3.JsonToJsonSchema
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.format.KondorJson

/**
 * Converts a API to OpenApi3 format JSON, using Kondor
 *
 * If you are using Jackson, you probably want to use ApiRenderer.Auto()!
 */
class KondorOpenApi3Renderer(
    private val json: KondorJson,
    private val refLocationPrefix: String = "components/schemas",
    private val jsonToJsonSchema: JsonSchemaCreator<JsonNode, JsonNode> = JsonToJsonSchema(json, refLocationPrefix),
) : ApiRenderer<Api<JsonNode>, JsonNode> by OpenApi3ApiRenderer(json, refLocationPrefix, jsonToJsonSchema) {

    override fun toSchema(obj: Any, overrideDefinitionId: String?, refModelNamePrefix: String?): JsonSchema<JsonNode> =
        when (obj) {
            is JsonNode -> jsonToJsonSchema.toSchema(obj, overrideDefinitionId, refModelNamePrefix)
            is Enum<*> -> toEnumSchema(obj, refModelNamePrefix, overrideDefinitionId)
            else -> try {
                toObjectSchema(obj, refModelNamePrefix, overrideDefinitionId)
            } catch (e: Exception) {
                toFallbackSchema(refModelNamePrefix, overrideDefinitionId)
            }
        }

    private fun toEnumSchema(
        obj: Enum<*>,
        refModelNamePrefix: String?,
        overrideDefinitionId: String?,
    ): JsonSchema<JsonNode> {
        val newDefinition = json.obj(
            "example" to json.string(obj.name),
            "type" to json.string("string"),
            "enum" to json.array(obj.javaClass.enumConstants.map { json.string(it.name) })
        )
        val definitionId =
            (refModelNamePrefix.orEmpty()) + (overrideDefinitionId ?: ("object" + newDefinition.hashCode()))
        return JsonSchema(
            json { obj("\$ref" to string("#/$refLocationPrefix/$definitionId")) },
            setOf(definitionId to newDefinition)
        )
    }

    private fun toObjectSchema(
        obj: Any,
        refModelNamePrefix: String?,
        overrideDefinitionId: String?,
    ): JsonSchema<JsonNode> {
        val schema = json.converterFor(obj).schema()
        val definitionId = overrideDefinitionId ?: obj::class.simpleName ?: return JsonSchema(schema)

        val reference = (refModelNamePrefix ?: "") + definitionId
        val schemaRef = json {
            obj("\$ref" to string("#/$refLocationPrefix/$reference"))
        }

        return JsonSchema(schemaRef, setOf(reference to schema))
    }

    private fun toFallbackSchema(
        refModelNamePrefix: String?,
        overrideDefinitionId: String?,
    ) = jsonToJsonSchema.toSchema(json.obj(), overrideDefinitionId, refModelNamePrefix)
}
