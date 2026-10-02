# AGENTS.md

Caveats of this project that are not obvious from the code. Read them before changing the affected areas.

## The ITK agent is copied from a2a-jakarta

The `itk` module is adapted from the
[`itk` module of a2a-jakarta](https://github.com/wildfly-extras/a2a-jakarta/tree/main/itk), copied at
[`864aace`](https://github.com/wildfly-extras/a2a-jakarta/tree/864aace54fbe4a6e62b3d3dcff411ccf722279fe/itk)
(2026-09-10). It is copied rather than depended on: upstream does not publish `a2a-jakarta-itk`, and its WAR
bundles the server jars that the feature-pack provides as modules.

Keep it in sync whenever `version.org.wildfly.a2a` or `version.sdk` is upgraded: diff upstream's `itk` directory
between the recorded commit and the new release, port the changes, and update the commit above.

Intentional differences to preserve while porting:

- The agent classes live in `org.wildfly.a2a.itk` instead of `org.wildfly.a2a.jakarta.itk`.
  `org.a2aproject.sdk.itk.Main` keeps upstream's package, as the a2a-itk launcher hard-codes that class name.
- `Main` starts the server with `--stability=experimental` instead of `preview`, the level of the feature-pack
  layers.
- `itk/pom.xml` provisions the A2A feature-pack and declares the SDK and Jakarta artifacts as `provided`. Only the
  v0.3 compat client, which the feature-pack does not ship, is bundled in the WAR. A `pom`-type dependency on the
  feature-pack exists solely so that `mvn -Pitk -pl itk -am install` builds it.
- `microprofile-config.properties` raises `a2a.executor.core-pool-size`. Upstream does not need this, because
  there a2a-jakarta's `AsyncManagedExecutorServiceProducer` replaces the SDK's executor with the container's
  `ManagedExecutorService`. Loaded from a JBoss module, that alternative is never selected, so the deployment
  runs on the SDK's own pool, which only grows past its 5 core threads once its queue of 100 is full.
- `run_itk.sh` and `.github/workflows/itk.yml` follow upstream, without the handling for SNAPSHOT SDK versions.
