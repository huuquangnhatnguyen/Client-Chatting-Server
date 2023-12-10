# Client-Chatting-Server
This is a socket programming project to create a server to multiple clients chatting boards.

##Compiling Instructions:

Using VS Code:

1) Open VS code from a folder containing Server.java and Clienta.java
2) Run Server.java
3) Run Client.java
4) Create a username
5) Connect to the server using %connect localhost 1234
6) You have now connected to server

##Major Issues:

Our groups major issue was figuring out how to split the single server into multiple private messaging boards.
The solution we found was to use a hashmap to store 5 different instances of the server
allowing us to create 5 different private messaging boards.

Another issue we ran into was in the first iteration leaving and joining a chat would not notify the rest
of the messaging board. To fix this issue, we added in a function that would broadcast the joining and leaving method 
to the entire private board the current user is in.

The final major issue our group faced was the makefile. We tried to have the makefile run the server, then open 3 instances of 
the client in different terminals, so all could be used to do different acvities. However, throughout testing we could not
get the new terminals to open, only the files to be opened.
