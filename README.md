<img src="vipslogo_512.png" alt="VIPS Logo" height="250"/>

# VIPS Weather Proxy
This is a collection of web service endpoints that can retrieve weather data from various sources in a uniform way, and returns the data in standard VIPS weatherdata format.

The services can be investigated with Swagger at `[live-site-hostname]/q/swagger-ui`

Currently  (2024-11-04), the weather data sources include:
* [A-Lab](http://a-lab.fi/en-home) (Finland)
* [Davis](https://www.davisinstruments.com/collections/weather-stations) (via [FruitWeb](https://www.fruitweb.info/en/), Germany)
* [Meteobot](https://meteobot.com/en/) weather stations
* [Metos/FieldClimate/Pessl](https://metos.global/en/) weather stations
* [Open-Meteo](https://open-meteo.com/) world wide service (location/grid)
* [Tahmo](https://tahmo.org/) weather stations (Africa)
* [USPest](https://uspest.org/) weather stations (USA)
* [VitiMeteo](https://www.agrometeo.ch/) agrometeorological network (Switzerland)
* [Met Norway](https://www.met.no/en) location forecast (world wide)


This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Generating sources for DMIWeatherService
This has been done by 
1. Place the WSDL file in the `src/main/resources/wsdl` directory
2. Add the Quarkus version of Apache CXF to pom.xml
```xml
 <dependency>
    <groupId>io.quarkiverse.cxf</groupId>
    <artifactId>quarkus-cxf</artifactId>
</dependency>
 ```
3. Add the generated sources directory to the Maven build process
```xml
<build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                <execution>
                    <id>add-source</id>
                    <phase>generate-code</phase>
                    <goals>
                    <goal>add-source</goal>
                    </goals>
                    <configuration>
                    <sources>
                        <source>target/generated-sources/wsdl2java</source>
                    </sources>
                    </configuration>
                </execution>
                </executions>
            </plugin>
```
And if you're developing this application with VSCode, you can add the sources to the classpath by clicking on `JAVA PROJECTS -> ... -> Configure classpath` and add `target/generated-sources/wsdl2java` to make the editor happy.


## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/vipsweatherproxy-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
