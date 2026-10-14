# PissMod

A Fabric mod for Minecraft **26.2**, inspired by the Garry's Mod "Pee SWEP". Your character pees from the crotch.

## How it works

Hold **Z** (rebindable under *Options → Controls → PissMod*):

1. **Key down:** you hear your pants unzip.
2. The stream starts with its own sound, then keeps looping for as long as you hold the key.
3. **Key up:** the stream tapers off and dribbles, then your pants zip back up.

Everyone nearby sees the stream and hears the sounds. The stream is drawn as droplets that follow a physics arc and splash where they land. It also puts out fire, campfires and burning mobs.

## Stream power (0–50)

Press **K** to open the settings screen, or edit `config/pissmod.json`.

| Power | Effect |
|-------|--------|
| 0 | Normal peeing (the default). A gentle arc that lands about 2–3 blocks ahead. |
| 15–29 | Stronger and longer. Above 20 it starts pushing mobs and players. |
| **30+** | **Hurts whoever it hits**: 0.5 hearts per hit at 30, up to 2 hearts per hit at 50. |
| 50 | Firehose. Nearly straight. About 15 blocks looking ahead, 30+ blocks aimed upward. |

You can also change the stream color on the settings screen.

### `config/pissmod.json`

```json
{
  "streamPower": 0,            // your power, 0-50
  "streamColor": "#F2D53C",    // your stream color
  "maxStreamPower": 50,        // server: highest power anyone may use
  "damageThreshold": 30,       // server: power at which the stream starts hurting
  "damageAtThreshold": 1.0,    // server: damage per hit at the threshold (half-hearts)
  "damageAtMaxPower": 4.0,     // server: damage per hit at power 50 (half-hearts)
  "knockback": true,           // server: strong streams push entities
  "extinguishFire": true       // server: the stream puts out fires
}
```

The `server` options are read from the config of whoever hosts the world. In singleplayer, that's your own file. Damage follows the server's PvP setting.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25

You need to install the mod on both the client and the server.

## Building

```sh
./gradlew build
```

The jar is written to `build/libs/pissmod-<version>.jar`.

## Credits

All sounds were synthesized for this mod. None of the original addon's sound files are used.
