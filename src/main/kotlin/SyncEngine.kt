package org.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Simulates a single client device. Each [SyncEngine] owns exactly one [Record] and keeps
 * its local copy in sync with the remote copy on the [remoteServer].
 */
class SyncEngine(
    private val remoteServer: RemoteServer,
    private var _localRecord: Record
) {
    private var running: Boolean = true
    private var sleepTime: Long = 1000L

    var localRecord: Record
        get() = _localRecord
        set(value) { _localRecord = value }

    var remoteRecord: Record? = null

    init {
        runEventLoop()
    }

    private fun runEventLoop() {
        CoroutineScope(Dispatchers.IO).launch {
            while (running) {
                // Get remote record
                val getRequest = HttpRequest(HttpMethod.GET, _localRecord)
                val getResponse = simulateNetworkFailure {
                    remoteServer.httpRequest(getRequest)
                }
                if (getResponse == null) {
                    delay(sleepTime.milliseconds)
                    continue
                }

                when (getResponse.status) {
                    404 -> {
                        // Create record at remote
                        if (!_localRecord.deleted) {
                            val postRequest = HttpRequest(HttpMethod.POST, _localRecord)
                            simulateNetworkFailure {
                                println("Creating new record ${_localRecord.id}")
                                remoteServer.httpRequest(postRequest)
                            }
                        }
                    }
                    200 -> {
                        val recordFromRemote = getResponse.record
                        if (recordFromRemote != null) {
                            val current = _localRecord
                            if (recordFromRemote.lastModified < current.lastModified) {
                                // Local is newer
                                val patchRequest = HttpRequest(HttpMethod.PATCH, current)
                                simulateNetworkFailure {
                                    println("Pushing changes for record ${current.id}")
                                    remoteServer.httpRequest(patchRequest)
                                }
                            } else if (recordFromRemote.lastModified > current.lastModified) {
                                // Remote is newer
                                println("Pulling changes for record ${current.id}")
                                _localRecord = current.copy(
                                    data = recordFromRemote.data,
                                    lastModified = recordFromRemote.lastModified
                                )
                            }
                        }
                    }
                }

                if (_localRecord.deleted) {
                    val deleteRequest = HttpRequest(HttpMethod.DELETE, _localRecord)
                    val deletedResponse = simulateNetworkFailure {
                        println("Deleting record ${_localRecord.id}")
                        remoteServer.httpRequest(deleteRequest)
                    }
                    if (deletedResponse != null && deletedResponse.status == 200) {
                        // Ending the event loop for the record simulates the record getting deleted
                        stopEventLoop()
                    }
                }

                delay(sleepTime.milliseconds)
            }
        }
    }

    private inline fun simulateNetworkFailure(block: () -> HttpResponse): HttpResponse? {
        // Fail requests 10% of the time
        val chance = 0.1f
        if (Random.nextFloat() < chance) {
            sleepTime = increaseExponentially(sleepTime)
            return null
        }

        val response = block.invoke()

        if (response.status == 200 || response.status == 201) {
            remoteRecord = response.record
        }

        sleepTime = 1000L
        return response
    }

    // Increase the retry time exponentially to not cause server overload
    private fun increaseExponentially(value: Long): Long {
        val reducedValue = value / 1000
        return 2.0.pow(reducedValue.toDouble()).toLong() * 1000
    }

    fun stopEventLoop() {
        running = false
    }
}