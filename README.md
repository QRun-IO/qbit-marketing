# QBit Marketing

[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21+-blue.svg)](https://adoptium.net/)

> QQQ QBit: marketing for QQQ applications.

**Status:** design: a design spec still to be published in QRun-IO/qqq docs/specs; implementation in progress. Nothing is published yet.

## Requirements

- Java 21.
- QQQ 4.1 (built against `4.1.0-RC.4`).

## Install

Not yet published. Once released, add `com.kingsrook.qbits:qbit-marketing` and produce the qbit into your `QInstance`:

```java
new MarketingQBitProducer()
   .withQBitConfig(new MarketingQBitConfig().withBackendName("rdbms"))
   .produce(qInstance);
```

## Naming

Tables are `marketing_<name>` (lowercase snake_case, backend name equal to the table name). Permissions are `marketing.<action>`.

## Adopting in another app

Follow the QQQ [adoption checklist](https://github.com/QRun-IO/qqq/blob/develop/docs/migration/adoption-checklist.adoc) once the first release is published.

## Build

```bash
./mvnw verify
```

Always use the Maven wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
