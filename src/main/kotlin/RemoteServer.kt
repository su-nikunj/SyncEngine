package org.example

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class RemoteServer {
    private val records = ConcurrentHashMap<Int, Record>()
    // Only allow 1000 requests to the server at the same time
    private val requestSemaphore = Semaphore(1000)

    private fun addRecord(record: Record): HttpResponse {
        records.putIfAbsent(record.id, record.copy())
        return HttpResponse(201, record)
    }

    private fun getRecordById(id: Int): Record? {
        return records[id]?.copy()
    }

    private fun updateRecord(updatedRecord: Record): HttpResponse {
        val id = updatedRecord.id
        if (!records.containsKey(id))
            return HttpResponse(404)

        records[id] = updatedRecord.copy()
        return HttpResponse(200, records[id])
    }

    private fun deleteRecord(id: Int): HttpResponse {
        val result = records.remove(id)
        return if (result != null)
            HttpResponse(200, result)
        else
            HttpResponse(404)
    }

    suspend fun httpRequest(request: HttpRequest): HttpResponse = requestSemaphore.withPermit {
        when (request.method) {
            HttpMethod.GET -> {
                val record = getRecordById(request.record.id) ?: return@withPermit HttpResponse(404)
                return@withPermit HttpResponse(200, record)
            }
            HttpMethod.POST -> addRecord(request.record)
            HttpMethod.PATCH -> updateRecord(request.record)
            HttpMethod.DELETE -> deleteRecord(request.record.id)
        }
    }

    // Synchronously modify a record for the purpose of the simulation
    fun updateRecordSync(updatedRecord: Record) {
        updateRecord(updatedRecord)
    }
}