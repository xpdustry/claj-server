# Copy Link and Join

Provides the ability to transfer packets between Mindustry clients for network play.

> [!WARNING]
> Now deprecated, please use the [CLaJ v2](https://github.com/xpdustry/claj) version instead.

## How to use

Everything you need to connect is in my [Scheme Size](https://github.com/xzxADIxzx/Scheme-Size) mod.
Just host the game and create a room in a special dialog next to the open server button, then copy the link and send it to your friend who can connect using it.

## How to host server

> You will need java 17

Download the repository and compile using the `./gradlew shadowJar` command.
Then using the jar located at `build/libs/claj-server.jar`, start the server with the command `java -jar claj-server.jar port` and replace port with the one you need.
For local testing, the command `./gradlew runClajServer` is provided, this will be available at the port `8000`.

## Local definitions

host - the player who hosted the game and created a room on the server.   
server - a remote machine that transmits packets from a client to a host.   
client - a player connecting via a link to a host through a server.   
link - a string containing the room key, ip and port of the server.   
room - an object that exists only on the server, serving as a container for connections to the host and client.
