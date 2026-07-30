package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class RemoteServer {
    private val records = mutableListOf<Record>()
    // Only allow 1000 requests to the server at the same time
    private val requestSemaphore = Semaphore(1000)
    // Only allow 1 write operation at a time
    private val writeDispatcher = Dispatchers.IO.limitedParallelism(1)

    private suspend fun addRecord(record: Record): HttpResponse = withContext(writeDispatcher) {
        records.add(record.copy())
        return@withContext HttpResponse(201, record)
    }

    private suspend fun getRecordById(id: Int): Record? = withContext(writeDispatcher) {
        return@withContext records.find { it.id == id }?.copy()
    }

    private suspend fun updateRecord(updatedRecord: Record): HttpResponse = withContext(writeDispatcher) {
        val index = records.indexOfFirst { it.id == updatedRecord.id }
        if (index == -1)
            return@withContext HttpResponse(404)

        records[index] = updatedRecord.copy()
        return@withContext HttpResponse(200, records[index])
    }

    private suspend fun deleteRecord(id: Int): HttpResponse = withContext(writeDispatcher) {
        val index = records.indexOfFirst { it.id == id }
        if (index != -1)
            records.removeAt(index)
        return@withContext HttpResponse(200)
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
        val index = records.indexOfFirst { it.id == updatedRecord.id }
        if (index != -1) {
            records[index] = updatedRecord.copy()
        }
    }
}