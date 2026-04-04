# Please Chop

`Please Chop` is a NeoForge 1.21.1 mod scaffold for a lumberjack workstation mod. It currently starts from the `PleaseStore` project structure so the build, registration, and resource layout stay aligned with the other mods in this workspace.

![CurseForge thumbnail](docs/media/curseforge-thumbnail.png)

## Status

- Repository scaffold created from `PleaseStore`
- Mod id renamed to `pleasechop`
- Detailed architecture proposal lives in `docs/architecture-proposal.md`

## Settings

- The mod uses NeoForge's built-in config screen.
- Open the Mods list, select `Please Chop`, and press `Config` to adjust settings.
- Final config options will depend on the agreed lumberjack behavior.

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.221`

## Development

```bash
./gradlew runClient
./gradlew build
```
