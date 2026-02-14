# wildfly-a2a-feature-pack

> This is an unsupported experimental POC implementation. Use with caution.

A WildFly feature pack implementing the open-standard Agent2Agent (A2A) Protocol to enable universal interoperability and seamless collaboration between AI agents across diverse frameworks and vendors.

## Layers

The feature pack provides the following Galleon layers:

| Layer | Description |
|-------|-------------|
| `a2a` | Base layer with A2A subsystem |
| `a2a-client-jsonrpc` | A2A client with JSON-RPC 2.0 over HTTP transport |
| `a2a-client-grpc` | A2A client with gRPC binary protocol transport |
| `a2a-client-http-json` | A2A client with RESTful HTTP/JSON transport |
| `a2a-server-jsonrpc` | A2A server with JSON-RPC 2.0 over HTTP transport |
| `a2a-server-grpc` | A2A server with gRPC binary protocol transport |
| `a2a-server-http-json` | A2A server with RESTful HTTP/JSON transport |

Server layers include both the A2A SDK transport module and the Jakarta EE implementation. Client layers include only the SDK transport module.

## Provisioning with Maven

Use the `wildfly-maven-plugin` to provision WildFly with the A2A feature pack.

### A2A Server with All Transports (JSON-RPC, gRPC, REST)

For gRPC support, include the [wildfly-grpc-feature-pack](https://github.com/wildfly-extras/wildfly-grpc-feature-pack):

```xml
<plugin>
    <groupId>org.wildfly.plugins</groupId>
    <artifactId>wildfly-maven-plugin</artifactId>
    <version>5.0.0.Final</version>
    <configuration>
        <feature-packs>
            <feature-pack>
                <groupId>org.wildfly</groupId>
                <artifactId>wildfly-galleon-pack</artifactId>
                <version>39.0.1.Final</version>
            </feature-pack>
            <feature-pack>
                <groupId>org.wildfly.extras.grpc</groupId>
                <artifactId>wildfly-grpc-feature-pack</artifactId>
                <version>0.1.17</version>
            </feature-pack>
            <feature-pack>
                <groupId>org.wildfly.extras.a2a</groupId>
                <artifactId>wildfly-a2a-feature-pack</artifactId>
                <version>0.0.1.Alpha1-SNAPSHOT</version>
            </feature-pack>
        </feature-packs>
        <layers>
            <layer>cloud-server</layer>
            <layer>a2a-server-jsonrpc</layer>
            <layer>a2a-server-grpc</layer>
            <layer>a2a-server-http-json</layer>
            <layer>grpc</layer>
        </layers>
        <galleon-options>
            <jboss-fork-embedded>true</jboss-fork-embedded>
            <stability-level>preview</stability-level>
        </galleon-options>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>package</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Running Integration Tests

> To pass tests, a GRPC with fix for https://github.com/wildfly-extras/wildfly-grpc-feature-pack/issues/430 is required.

Based on [a2a-java-sdk-server-jakarta](https://github.com/wildfly-extras/a2a-java-sdk-server-jakarta) testing approach.

```bash
mvn clean install                                  # All tests
mvn clean test -pl testsuite/integration/jsonrpc   # JSON-RPC only
mvn clean test -pl testsuite/integration/grpc      # gRPC only
mvn clean test -pl testsuite/integration/http-json # HTTP-JSON only
```

## Running the TCK

The feature pack includes a TCK (Test Compatibility Kit) module that runs the official A2A protocol TCK against WildFly.

### Prerequisites

- Java 17+
- Maven 3.9+
- Python 3.11+ with `uv` package manager

### Build and Run TCK

1. Build the feature pack:
   ```bash
   mvn clean install
   ```

2. Build and provision the TCK server:
   ```bash
   mvn package -pl tck -am
   ```

3. Start the WildFly server:
   ```bash
    SUT_JSONRPC_URL=http://localhost:8080 SUT_GRPC_URL=http://localhost:9555 SUT_REST_URL=http://localhost:8080 tck/target/wildfly/bin/standalone.sh --stability=preview
   ```

4. Clone and set up the A2A TCK:
   ```bash
   git clone https://github.com/a2aproject/a2a-tck.git
   cd a2a-tck
   pip install uv
   uv pip install -e .
   ```

5. Run the TCK:
   ```bash
   TCK_STREAMING_TIMEOUT=4.0 SUT_JSONRPC_URL=http://localhost:8080 SUT_GRPC_URL=http://localhost:9555 SUT_REST_URL=http://localhost:8080 uv run ./run_tck.py --sut-url http://localhost:8080 --category all --transports jsonrpc,grpc,rest --compliance-report report.json
   ```

6. Stop the server:
   ```bash
   mvn wildfly:shutdown -pl tck
   ```

### TCK Endpoints

- JSON-RPC: `http://localhost:8080`
- REST (HTTP-JSON): `http://localhost:8080`
- gRPC: `localhost:9555`

## Resources

- [A2A Protocol](https://a2aprotocol.ai/) - Official A2A protocol specification
- [A2A](https://github.com/a2aproject/A2A) - Reference implementation of the A2A protocol
- [a2a-java](https://github.com/a2aproject/a2a-java) - Java SDK for A2A protocol
- [a2a-tck](https://github.com/a2aproject/a2a-tck) - Official A2A protocol TCK
- [a2a-java-sdk-server-jakarta](https://github.com/wildfly-extras/a2a-java-sdk-server-jakarta) - Jakarta EE server implementation of A2A Java SDK
- [wildfly-ai-feature-pack](https://github.com/wildfly-extras/wildfly-ai-feature-pack) - WildFly feature pack for AI/LLM integration (langchain4j)
