# Combat progression foundation · demo

## Persistent shield rules

- Talent shield has no timer and does not decay when a room advances.
- It accumulates across room transitions during one expedition.
- Its safety ceiling is 100% of the character's current maximum health.
- A new expedition and a return to camp clear temporary combat shields.
- Existing soul and career ward pools retain their source rules; they are no longer reset merely because a new room is generated.
- Shield amount and ward pools are serialized. Save v14 clamps legacy shield values to the new safe ceiling and validates new snapshots against current maximum health.

## Enemy growth ceiling

- Threat rating ceiling raised from 6 to 12.
- Endless-depth contribution ceiling raised from 2.5 to 4.5, still growing by 0.10 per floor until it reaches the ceiling.
- Enemy attack reach/range scaling ceiling raised from +20% to +40%.
- Enemy movement scaling ceiling raised from +45% to +75%.
- Save validation expanded for threat, encounter floors, and enemy health to preserve high-growth encounters without accepting unbounded values.
- Entry-locked scaling remains unchanged: living enemies are not retroactively repriced by mid-room buffs or current player health.

## Remaining staged work

This commit is the combat-foundation phase. Follow-on phases:
1. Five universal active skills and five matching follow-ups, with career and soul cross-synergies.
2. Three soul arts per bound soul: damage, control, and enhancement.
3. Five-branch talent tree and deeper camp progression/mastery.
4. Expanded relic pool, upgrades, and two-relic combination effects.
5. UI, save migrations, and regression coverage for all new build paths.
