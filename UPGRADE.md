# Upgrading

## Unreleased

No schema change.

- The help bundle is now applied at run time, so tables show their help (including how rows get created) when empty. A host's own table help is kept.
- New config `staleChannelRulesDays` (default 90). Check Readiness on a campaign now also warns when a channel its content uses has a `rulesVerifiedDate` older than that. Hosts that do not want it set a large number.
- New hosts: run **Load Verified Starter Data** once to get the starter channels, the `default` policy and the starter products.
