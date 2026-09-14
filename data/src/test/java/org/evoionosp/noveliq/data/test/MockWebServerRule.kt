package org.evoionosp.noveliq.data.test

import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MockWebServerRule : TestWatcher() {
    val server = MockWebServer()

    override fun starting(description: Description?) {
        server.start()
    }

    override fun finished(description: Description?) {
        server.shutdown()
    }

    fun baseUrl(): String = server.url("/").toString()

    fun enqueueJson(
        code: Int,
        body: String,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )
    }

    fun enqueuePlainText(
        code: Int,
        body: String,
    ) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "text/plain"),
        )
    }

    fun takeRequest(): RecordedRequest = server.takeRequest(5, TimeUnit.SECONDS)!!
}
