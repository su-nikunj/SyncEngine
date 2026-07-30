# Sync Engine

A kotlin cli program that simulates synchronization between a central servers and a bunch of local devices. Each local device runs its own coroutine, so it will consume a lot of resources for large numbers such as million.
The central server sends and receives requests in HTTP status codes to simulate a real server.
