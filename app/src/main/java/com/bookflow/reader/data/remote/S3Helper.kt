package com.bookflow.reader.data.remote

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.writeToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class S3Helper(private val region: String) {

    suspend fun uploadFile(bucketName: String, objectKey: String, file: File): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                S3Client { this.region = this@S3Helper.region }.use { s3 ->
                    val request = PutObjectRequest {
                        this.bucket = bucketName
                        this.key = objectKey
                        this.body = aws.smithy.kotlin.runtime.content.ByteStream.fromBytes(file.readBytes())
                    }
                    s3.putObject(request)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun downloadFile(bucketName: String, objectKey: String, destinationFile: File): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                S3Client { this.region = this@S3Helper.region }.use { s3 ->
                    val request = GetObjectRequest {
                        this.bucket = bucketName
                        this.key = objectKey
                    }
                    s3.getObject(request) { response ->
                        response.body?.writeToFile(destinationFile)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
