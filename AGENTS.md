# Project rules

- Current demo is a native Android Java real-time action RPG. main retains the earlier turn-based prototype.
- Action app ID io.wayfarer.rpg.action is separate from the legacy app. Preserve legacy saves; do not silently reinterpret them as action snapshots.
- Preserve existing game/save IDs and schema; add explicit migrations for incompatible changes.
- Keep core gameplay free of Android imports. UI issues belong in the adapters, not duplicated gameplay logic.
- Run `bash scripts/test-core.sh` for gameplay/persistence edits; run Android lint/build when SDK access exists, or use the CI result and explicitly report what has not been verified.
- Never call a build, device test or gameplay check successful without actual evidence.
- Do not commit keystores, credentials, local.properties, generated build output or debug APKs.
- Preserve all three manual save slots when starting a new game. Do not silently discard corrupt saves.
- Use the current branch as the basis of edits; never replace existing code with an unrelated older scaffold.

