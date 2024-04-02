# Extended TFTP Server

This project implements an extended TFTP (Trivial File Transfer Protocol) server and client in Java. The server supports multiple users to upload, download, and manage files, while the client provides a command-line interface for interacting with the server.

## Features

 - Thread-Per-Client (TPC) server pattern
 - Support for multiple clients
 - File upload and download
 - Directory listing
 - File deletion
 - User login and logout
 - Broadcast notifications for file additions and deletions


## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or later
- Apache Maven

### Building

1. Clone the repository
2. Edit server.bat file : Line 4 cd <"Directory to your /server">

### Running
1. Start server.bat to start the server.
2. To Start a test client go to client\Tftpclinet.exe.
3. You can run as many clients as you want.

### Commands

The client provides the following commands for interacting with the server:

- `LOGRQ <Username>`: Login a user to the server.
- `DELRQ <Filename>`: Delete a file from the server.
- `RRQ <Filename>`: Download a file from the server to the current working directory.
- `WRQ <Filename>`: Upload a file from the current working directory to the server.
- `DIRQ`: List all filenames in the server's Files folder.
- `DISC`: Disconnect from the server and close the client program.

## Authors:
Matan Elkaim & Adi Shugal
