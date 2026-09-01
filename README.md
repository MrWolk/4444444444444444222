# ALLINFactions 1.0.0

Faction system for ALLINONLINE, Paper 26.2 / Java 25.

## Factions
- Inquisition: warnings, wanted list, inspection, UUID-bound baton, arrest, online-time jail, prison cauldron work.
- Thieves Guild: theft, persistent statistics, contraband routes using admin-defined beacons.
- Workers: online-time promotion and public API for Miner/Farmer/Fishing x2 normal-loot bonuses.
- Shared: SQLite, treasuries, hourly salaries, salary debt, leaders, blacklist, faction chat, dynamic GUI, admin commands, backups.

## Build
The repository contains `.github/workflows/build.yml`. Upload the project to GitHub and run **Actions -> Build ALLINFactions -> Run workflow**. The artifact contains `ALLINFactions-1.0.0.jar`.

Build target uses Java 25 and `io.papermc.paper:paper-api:26.2.build.121-stable`.

## Runtime dependencies
- An economy plugin that registers a Vault-compatible Economy service.
- Vault / VaultUnlocked-compatible Vault API provider on the server.

Without an economy provider, faction membership still loads, but money mechanics cannot pay/withdraw.

## First setup
1. Put the jar into `plugins/` and restart.
2. Ensure Vault-compatible economy is loaded.
3. Assign leaders:
   - `/fadmin setleader inquisition <nick>`
   - `/fadmin setleader thieves <nick>`
   - `/fadmin setleader workers <nick>`
4. Stand in the prison and run `/fadmin prison setspawn`.
5. Add prison work cauldrons with `/fadmin prison toilet add`.
6. Add contraband route points while standing at the beacon locations:
   - `/fadmin thieves beacon start add`
   - `/fadmin thieves beacon finish add`
7. Fund faction treasuries, e.g. `/fadmin treasury workers add 50000`.

## Main commands
`/faction`, `/f`, `/fc`, `/warn`, `/warns`, `/search`, `/jailtime`, `/steal`, `/fadmin`.

## Worker profession API
Other ALLIN profession plugins can retrieve the service:

```java
RegisteredServiceProvider<ALLINFactionsAPI> rsp =
    Bukkit.getServicesManager().getRegistration(ALLINFactionsAPI.class);

if (rsp != null) {
    LootBonusResult result = rsp.getProvider().applyProfessionBonus(
        player,
        Profession.MINER,
        RewardType.NORMAL,
        rewardAmount
    );
    rewardAmount = result.finalAmount();
}
```

Use `RewardType.SPECIAL` for rare bonus rewards that must not be doubled.

## Important data files
- `plugins/ALLINFactions/config.yml`
- `plugins/ALLINFactions/data.db`
- `plugins/ALLINFactions/backups/`

Do not delete `data.db` when updating the jar.
