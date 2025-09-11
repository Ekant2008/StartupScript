# StartupScript

A simple Java project using **Helidon SE** that starts a configurable web server.

## Requirements
- Java 21+
- Maven 3.9+
- Git

## Build

Clone and build the project:

```bash
git clone https://github.com/<your-username>/StartupScript.git
cd StartupScript
mvn clean install

##Windows PowerShell

mvn exec:java "-Dexec.mainClass=com.one211.startupscript.ServerStartupScript" "-Dserver.port=9091" "-Dserver.tls.enabled=true"
