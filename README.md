# Client-Chatting-Server
This is a socket programming project to create a server to multiple clients chatting boards.

## Compiling Instructions:

Using VS Code:

1) Open VS code from a folder containing Server.java and Clienta.java
2) Run Server.java
3) Run Client.java
4) Create a username
5) Connect to the server using %connect localhost 1234
6) You have now connected to server

## Major Issues:

The major issue was figuring out how to split the single server into multiple private messaging boards.
The solution found was to use a hashmap to store 5 different instances of the server
allowing the server to create 5 different private messaging boards.

Another issue was in the first iteration leaving and joining a chat would not notify the rest
of the messaging board. To fix this issue, a function was added in to broadcast the joining and leaving method 
to the entire private board the current user is in.

