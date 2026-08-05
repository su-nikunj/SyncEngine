package org.example

import kotlin.random.Random
import kotlin.uuid.Uuid

fun main() {
    val remoteServer = RemoteServer()

    val numRecords = 100_000

    // Create one local device per record. Each SyncEngine instance simulates a
    // separate device, and each device syncs one single Record with the server.
    val localDevices = mutableListOf<SyncEngine>()
    for (i in 1..numRecords) {
        val data = Data(
            Random.nextInt(),
            Uuid.random().toString(),
            Random.nextFloat(),
            Random.nextBoolean()
        )
        val localRecord = Record(i, data, System.currentTimeMillis())
        val localEngine = SyncEngine(remoteServer, localRecord)

        localDevices.add(localEngine)
    }

    val prompt = "1. Modify some random local records.\n" +
            "2. Modify some random remote records.\n" +
            "3. Mark some local records as deleted.\n" +
            "4. Add a bunch of new local records.\n" +
            "5. Print data\n" +
            "6. Reprint the prompt"
    println(prompt)

    var input: Int?
    do {
        // Pick 20% of values to modify
        val amountToPick = 20 * numRecords / 100

        print("Enter input: ")
        input = readln().toIntOrNull()

        when (input) {
            null -> break
            6 -> println(prompt)
            5 -> {
                for (syncEngine in localDevices) {
                    println("ID: ${syncEngine.localRecord.id}")
                    println("Local Record:")
                    println(syncEngine.localRecord)
                    println("Remote Record:")
                    println(syncEngine.remoteRecord)
                    println("-----------------------------")
                }
            }
            4 -> {
                for (i in (localDevices.size + 1)..(localDevices.size + amountToPick)) {
                    val newData = Data(
                        Random.nextInt(),
                        Uuid.random().toString(),
                        Random.nextFloat(),
                        Random.nextBoolean()
                    )
                    val newRecord = Record(i, newData, System.currentTimeMillis())
                    localDevices.add(SyncEngine(remoteServer, newRecord))
                    println("New Record: $i")
                }
            }
            3 -> {
                val engines = localDevices.shuffled().take(amountToPick)
                for (syncEngine in engines) {
                    syncEngine.localRecord = syncEngine.localRecord.copy(deleted = true)
                    println("${syncEngine.localRecord.id} deleted")
                }
            }
            2 -> {
                val engines = localDevices.shuffled().take(amountToPick)
                for (syncEngine in engines) {
                    if (!syncEngine.localRecord.deleted && syncEngine.remoteRecord != null) {
                        val newData = Data(
                            Random.nextInt(),
                            Uuid.random().toString(),
                            Random.nextFloat(),
                            Random.nextBoolean()
                        )
                        val newRecord = Record(
                            syncEngine.localRecord.id,
                            newData,
                            System.currentTimeMillis()
                        )

                        remoteServer.updateRecordSync(newRecord)
                        println("${newRecord.id} modified")
                    }
                }
            }
            1 -> {
                val engines = localDevices.shuffled().take(amountToPick)
                for (syncEngine in engines) {
                    if (!syncEngine.localRecord.deleted) {
                        val newData = Data(
                            Random.nextInt(),
                            Uuid.random().toString(),
                            Random.nextFloat(),
                            Random.nextBoolean()
                        )
                        val newRecord = Record(
                            syncEngine.localRecord.id,
                            newData,
                            System.currentTimeMillis()
                        )

                        syncEngine.localRecord = newRecord
                        println("${newRecord.id} modified")
                    }
                }
            }
        }
    } while (input in 1..<7)

    for (syncEngine in localDevices) {
        syncEngine.stopEventLoop()
    }
}