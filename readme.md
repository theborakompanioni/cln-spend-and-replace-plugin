[![Build](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/actions/workflows/build.yml)
[![GitHub Release](https://img.shields.io/github/release/theborakompanioni/cln-spend-and-replace-plugin.svg?maxAge=3600)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/releases/latest)
[![License](https://img.shields.io/github/license/theborakompanioni/cln-spend-and-replace-plugin.svg?maxAge=2592000)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/blob/master/LICENSE)


<p align="center">
    <img src="https://github.com/theborakompanioni/cln-spend-and-replace-plugin/blob/master/docs/assets/images/logo.png" alt="Logo" width="255" />
</p>


cln-spend-and-replace-plugin
===

## RPC commands

### `sar-listconfigs`
Command to list all configuration options.

```shell
user@host:~$ lightning-cli sar-listconfigs
{
   "dry-run": false
}
```

### `sar-ticker`
Get the ticker representing the current exchange rate for the provided currency.

```shell
user@host:~$ lightning-cli sar-ticker
{
   "result": {
      "BTC/USD": {
         "ask": "27308.20000",
         "bid": "27308.20000",
         "high": "28000.00000",
         "low": "27155.00000",
         "open": "27588.10000",
         "last": "27308.20000"
      }
   }
}
```

```shell
user@host:~$ lightning-cli sar-ticker GBP
{
   "result": {
      "BTC/GBP": {
         "ask": "21896.70000",
         "bid": "21896.70000",
         "high": "22496.10000",
         "low": "21803.60000",
         "open": "22201.40000",
         "last": "21901.50000"
      }
   }
}
```

### `sar-version`
Command to print the plugin version.

```shell
user@host:~$ lightning-cli sar-version
{
   "version": "0.1.0-dev.4.uncommitted+7f363fa"
}
```

## Development

### Requirements
- java >=17
- docker

### Build
```shell script
./gradlew build -x test
```

### Run
TBD
 
### Test
```shell script
./gradlew test integrationTest --rerun-tasks
```

### Dependency Verification
Gradle is used for checksum and signature verification of dependencies.

```shell script
# write metadata for dependency verification
./gradlew --write-verification-metadata pgp,sha256 --export-keys
```

See [Gradle Userguide: Verifying dependencies](https://docs.gradle.org/current/userguide/dependency_verification.html)
for more information.

### Checkstyle
[Checkstyle](https://github.com/checkstyle/checkstyle) with adapted [google_checks](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml)
is used for checking Java source code for adherence to a Code Standard.

```shell script
# check for code standard violations with checkstyle
./gradlew checkstyleMain --rerun-tasks
```

### SpotBugs
[SpotBugs](https://spotbugs.github.io/) is used for static code analysis.

```shell script
# invoke static code analysis with spotbugs
./gradlew spotbugsMain --rerun-tasks
```


## Contributing
All contributions and ideas are always welcome. For any question, bug or feature request, 
please create an [issue](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/issues). 
Before you start, please read the [contributing guidelines](contributing.md).


## Resources

- Bitcoin: https://bitcoin.org/en/getting-started
- Lightning Network: https://lightning.network
- Spring Boot (GitHub): https://github.com/spring-projects/spring-boot
- cln Plugin Docs: https://lightning.readthedocs.io/PLUGINS.html
---
- Bitcoin Core (GitHub): https://github.com/bitcoin/bitcoin
- cln (GitHub): https://github.com/ElementsProject/lightning ([Docker](https://hub.docker.com/r/elementsproject/lightningd))
- JRPClightning (GitHub): https://github.com/clightning4j/JRPClightning
- bitcoin-spring-boot-starter (GitHub): https://github.com/theborakompanioni/bitcoin-spring-boot-starter

## License

The project is licensed under the Apache License. See [LICENSE](LICENSE) for details.
