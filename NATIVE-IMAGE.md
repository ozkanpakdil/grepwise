## Building Native Image
First we need to update reachability json to latest, in order to create it automatically use command below
```shell
java -agentlib:native-image-agent=config-output-dir=./graalcnf/ -jar ./target/grepwise-0.0.1-SNAPSHOT.jar
```
after that we can build native image using command below
```shell
mvn -ntp -Pnative -DskipTests native:compile
```
