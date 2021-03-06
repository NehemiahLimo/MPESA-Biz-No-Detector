package com.jama.mpesa_biz_no_detector.azureVisionRest

import com.jama.mpesa_biz_no_detector.models.VisionResult
import com.jama.mpesa_biz_no_detector.utils.Constants
import com.jama.mpesa_biz_no_detector.utils.VisionException
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AzureVisionRest(
    azureVisionEndPoint: String,
    private val azureVisionKey: String,
    private val byteArray: ByteArray
) {

    private val baseUrl = "$azureVisionEndPoint${Constants.READ_API_ENDPOINT}"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val azureVisionService = retrofit.create(AzureVisionService::class.java)

    suspend fun startVision(): VisionResult? {
        val analyzedResponse = analyze()
        val headers = analyzedResponse.headers()
        val requestId =
            headers.get("apim-request-id") ?: throw VisionException("Request ID not found")

        var pool = true
        var visionResult: VisionResult? = null

        while (pool) {
            val analyzedResultResponse = analyzeResults(requestId)
            val body = analyzedResultResponse.body()!!
            when (body.status) {
                "succeeded" -> {
                    pool = false
                    visionResult = body
                }
                "failed" -> pool = false
            }
        }
        return visionResult
    }

    private suspend fun analyze(): Response<Void> {
        val contentType = Constants.CONTENT_TYPE
        val requestBody = RequestBody.create(
            MediaType.parse(contentType),
            byteArray
        )
        return azureVisionService.analyze(requestBody, contentType, azureVisionKey)
    }

    private suspend fun analyzeResults(requestId: String): Response<VisionResult> {
        return azureVisionService.analyzeResults(azureVisionKey, requestId)
    }
}