@echo off

rem Change directory
cd C:\Users\nemne\Downloads\SPL\Assignment3\Assignment3\server 

rem Run Maven compile and execute Java main class
mvn compile && mvn exec:java -Dexec.mainClass=bgu.spl.net.impl.tftp.TftpServer -Dexec.args=7777

rem Open a new command prompt window and keep it open
cmd /k