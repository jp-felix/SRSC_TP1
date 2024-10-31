#textdstp:

javac -d out src/main/java/dstp/config/CryptoConfigLoader.java src/main/java/dstp/DSTPSocket.java src/main/java/examples/multicast/MulticastReceiver.java src/main/java/examples/multicast/MulticastSender.java

#to test tftp

Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName } > sources.txt
javac -d out -sourcepath src @(Get-Content sources.txt)

test streaming:
java -cp out main.java.examples.streaming.hjStreamServer files/movies/cars.dat 224.2.2.2 9000 

######################