Quick test
==========

Start the dev client from the project root:

```bash
./gradlew runClient
```

What is already prepared:

- NeoForge 21.1.x dev runtime on Minecraft 1.21.x
- Minecraft assets downloaded into the local Gradle cache
- Runtime folder initialized at `run/`

First in-game check:

1. Create a Creative world.
2. Search for `Lumberjack Workstation` in the inventory.
3. Place any workstation variant near an unemployed villager and an existing village bed setup.
4. Wait through normal villager work hours and confirm the villager claims the workstation.
5. Confirm the villager profession name displays as `Lumberjack`.
6. Confirm the lumberjack profession texture appears on both villagers and zombie villagers.
7. Break and replace the workstation and confirm it drops itself and can be reclaimed.
8. Craft a workstation from one matching log and one iron axe, then confirm the crafted block matches that wood variant.

Useful paths:

- Run directory: `run/`
- Built jar: `build/libs/PleaseChop-1.0.1.jar`
