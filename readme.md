[![Build](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/actions/workflows/build.yml)
[![GitHub Release](https://img.shields.io/github/release/theborakompanioni/cln-spend-and-replace-plugin.svg?maxAge=3600)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/releases/latest)
[![License](https://img.shields.io/github/license/theborakompanioni/cln-spend-and-replace-plugin.svg?maxAge=2592000)](https://github.com/theborakompanioni/cln-spend-and-replace-plugin/blob/master/LICENSE)


<p align="center">
    <img src="https://github.com/theborakompanioni/cln-spend-and-replace-plugin/blob/master/docs/assets/images/logo-dark.svg#gh-light-mode-only" alt="Logo" width="256" />
    <img src="https://github.com/theborakompanioni/cln-spend-and-replace-plugin/blob/master/docs/assets/images/logo-light.svg#gh-dark-mode-only" alt="Logo" width="256" />
</p>


cln-spend-and-replace-plugin
===
Core Lightning :zap: plugin to immediately re-stack all outgoing sats from your node.

**Note**: Most code is still experimental - ~~**use with caution**~~ **do not use till v0.1.0 is reached**.
This project is under active development. Pull requests and issues are welcome.
[Look at the changelog](changelog.md) to track notable changes.

- [x] Place limit orders on exchange for all outgoing payments
- [ ] Withdraw via Lightning automatically

## RPC commands

### `snr-listconfigs`
Command to list all configuration options.

```shell
user@host:~$ lightning-cli snr-listconfigs
{
    "result": {
        "dry-run": false,
        "fiat-currency": {
          "default": "USD"
        },
        "exchange": {
          "name": "Kraken",
          "host": "api.kraken.com"
        }
    }
}
```

### `snr-version`
Command to print the plugin version.

```shell
user@host:~$ lightning-cli snr-version
{
    "result": {
        "version": "0.1.0-dev.4.uncommitted+7f363fa"
    }
}
```

### `snr-ticker`
Get the ticker representing the current exchange rate for the provided currency.

```shell
user@host:~$ lightning-cli snr-ticker
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
user@host:~$ lightning-cli snr-ticker GBP
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

### `snr-balance`
Get the balance of your account.

```shell
user@host:~$ lightning-cli snr-balance
{
   "result": {
      "_": {
         "id": null,
         "name": null,
         "balances": {
            "BTC": {
               "available": "0.0002100000",
               "available-for-withdrawal": "0.0002100000",
               "borrowed": "0",
               "depositing": "0",
               "frozen": "0",
               "loaned": "0",
               "total": "0.0002100000",
               "withdrawing": "0"
            },
            "USD": {
               "available": "42.1337",
               "available-for-withdrawal": "42.1337",
               "borrowed": "0",
               "depositing": "0",
               "frozen": "0",
               "loaned": "0",
               "total": "42.1337",
               "withdrawing": "0"
            }
         }
      },
      "margin": {
         "id": "margin",
         "name": "margin",
         "balances": {
            "BTC": {
               "available": "0.0002100000",
               "available-for-withdrawal": "0.0002100000",
               "borrowed": "0",
               "depositing": "0",
               "frozen": "0",
               "loaned": "0",
               "total": "0.0002100000",
               "withdrawing": "0"
            },
            "USD": {
               "available": "42.1337",
               "available-for-withdrawal": "42.1337",
               "borrowed": "0",
               "depositing": "0",
               "frozen": "0",
               "loaned": "0",
               "total": "42.1337",
               "withdrawing": "0"
            }
         }
      }
   }
}
```

### `snr-history`
Get the trade history of your account.

```shell
user@host:~$ lightning-cli snr-history
{
  "result" : {
    "open" : {
      "abcdef-00000-000001" : {
        "id" : "abcdef-00000-000001",
        "type" : "BID",
        "status" : "NEW",
        "is-open" : true,
        "is-final" : false,
        "original-amount" : "0.42",
        "remaining-amount" : "0.42",
        "limit-price" : "21.0",
        "asset-pair" : "BTC/USD",
        "ref" : "0",
        "date" : "2021-05-26T03:33:20Z",
        "timestamp" : 1622000000
      }
    },
    "closed" : {
      "abcdef-00000-000000" : {
        "id" : "abcdef-00000-000000",
        "type" : "BID",
        "order-id" : "abcdef",
        "price" : "21000.0",
        "original-amount" : "0.21",
        "asset-pair" : "BTC/USD",
        "ref" : "",
        "fee-amount" : "0.090103",
        "fee-currency" : "USD",
        "date" : "2021-05-14T13:46:40Z",
        "timestamp" : 1621000000
      }
    }
  }
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
- cln Plugin Docs: https://lightning.readthedocs.io/PLUGINS.html
- Spring Boot (GitHub): https://github.com/spring-projects/spring-boot
---
- cln (GitHub): https://github.com/ElementsProject/lightning ([Docker](https://hub.docker.com/r/elementsproject/lightningd))
- JRPClightning (GitHub): https://github.com/clightning4j/JRPClightning
- XChange (GitHub): https://github.com/knowm/XChange
- bitcoin-spring-boot-starter (GitHub): https://github.com/theborakompanioni/bitcoin-spring-boot-starter

## License

The project is licensed under the Apache License. See [LICENSE](LICENSE) for details.
