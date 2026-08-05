# Sync Engine

A Kotlin CLI program that simulates synchronization between a central server and many local client devices. Each device keeps a copy of a single record in sync with the server, using HTTP-style requests and responses to mimic a real server.

## Key idea: one device per record

The simulation treats **each `SyncEngine` instance as a separate device**. Every device is responsible for exactly **one `Record`**, syncing its local copy with the remote copy held by the central `RemoteServer`.

Concretely, `Main.kt` creates `100_000` records by default (`numRecords`), which means it simulates **100,000 devices**, each running its own polling coroutine.

## How synchronization works

Each device runs a polling event loop (`SyncEngine.kt`). On every cycle it:

1. **GET**s its record from the server.
2. Handles the response by status code:
   - **404** — the record doesn't exist remotely, so the device **POSTs** it to create it (unless the local copy is marked deleted).
   - **200** — the record exists. The device compares `lastModified` timestamps:
     - local is **newer** → pushes changes to the server with **PATCH**;
     - remote is **newer** → pulls by replacing its local copy.
3. If the local copy is marked `deleted`, the device sends a **DELETE** and stops its loop.

To simulate real-world flakiness, `simulateNetworkFailure` drops **10%** of requests and increases the retry delay exponentially (so the server doesn't get overloaded with retries).

### Conflict resolution

Conflicts are resolved by **last-write-wins**: the record with the more recent `lastModified` timestamp wins, regardless of which side changed it.

## Server behavior

`RemoteServer` keeps records in an in-memory store and receives `HttpRequest`. To stay realistic:

- A `Semaphore(1000)` caps the number of concurrent requests.
- Records live in a thread-safe `ConcurrentHashMap` keyed by record id, so reads are lock-free while concurrent writes to different records stay safe.

## Data model

- `Record(id, data, lastModified, deleted)`: the syncable entity a device owns.
- `Data(intData, stringData, floatData, booleanData)`: the payload a record carries.
- `HttpMethod` / `HttpRequest` / `HttpResponse`: a lightweight HTTP abstraction built on status codes (`200`, `201`, `404`).

## Interactive CLI

After starting, the program prints a menu (`Main.kt`). Enter a number to act on roughly 20% of the records at once:

| # | Action                                                  |
|---|---------------------------------------------------------|
| 1 | Modify some random local records (on-device changes)    |
| 2 | Modify some random remote records (server-side changes) |
| 3 | Mark some local records as deleted                      |
| 4 | Add a bunch of new local records (new devices)          |
| 5 | Print all local and remote record data                  |
| 6 | Reprint the prompt                                      |

Entering anything else (or Ctrl-D on an empty line) exits the program.

## Usage

Requires a JDK (toolchain 26).

```sh
./gradlew run    # compiles and starts the interactive simulation
./gradlew build  # compiles and runs tests without launching the CLI
```