package org.http4k.serverless

import com.amazonaws.services.lambda.runtime.Context
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.toUrlFormEncoded

/**
 * Function loader for ApiGatewayV1 Lambdas
 *
 * Use main constructor if you need to read ENV variables to make your HttpHandler and the AWS context
 */
class ApiGatewayV1FnLoader(input: AppLoader) : ApiGatewayFnLoader(ApiGatewayV1AwsHttpAdapter, input) {

    /**
     * Use this constructor if you just want to convert a standard HttpHandler
     */
    constructor(input: HttpHandler) : this(AppLoader { input })
}

/**
 * This is the main entry point for lambda invocations using the V1 payload format.
 * It uses the local environment to instantiate the HttpHandler which can be used
 * for further invocations.
 *
 * Use main constructor if you need to read ENV variables to make your HttpHandler and the AWS context
 */
abstract class ApiGatewayV1LambdaFunction(input: AppLoader) :
    AwsLambdaEventFunction(ApiGatewayV1FnLoader(input)) {

    /**
     * Use this constructor if you just want to convert a standard HttpHandler
     */
    constructor(input: HttpHandler) : this(AppLoader { input })
}

object ApiGatewayV1AwsHttpAdapter : AwsHttpAdapter<Map<String, Any>, Map<String, Any>> {
    private fun Map<String, Any>.toHttp4kRequest() =
        Request(
            Method.valueOf(getString("httpMethod") ?: error("method is invalid")),
            Uri.of(getString("path").orEmpty())
                .query((getStringMap("queryStringParameters")?.toList() ?: emptyList()).toUrlFormEncoded())
        )
            .headers(toHeaders())
            .body(toBody())

    override fun invoke(req: Map<String, Any>, ctx: Context) = runCatching { req.toHttp4kRequest() }

    override fun invoke(resp: Response) = mapOf(
        "statusCode" to resp.status.code,
        "headers" to resp.headers.toMap(),
        "body" to resp.body.payload.base64Encode(),
        "isBase64Encoded" to true
    )
}
