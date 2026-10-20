# HUD Menu (Fabric, Minecraft 1.21.11)

Press **Right Shift** in a world to open the menu. Drag any HUD element to move it.
Pick a module on the left; on the right set its size, text color, shadow, background on/off,
background color and opacity. Settings save to `config/hudmenu.json`.

Modules: FPS, CPS, Ping, Auto Sprint, Coordinates, Keystrokes, Potion Effects.

## Build the .jar

Easiest way (gets you the right Gradle wrapper and version numbers):

1. Go to https://fabricmc.net/develop/template and generate a template for Minecraft 1.21.11
   (any mod name, tick "Yarn mappings", Java 21).
2. Unzip it, then delete its `src` folder.
3. Copy this project's `src` folder into it.
4. Open a terminal in that folder and run:
   - Windows: `gradlew build`
   - Mac/Linux: `./gradlew build`
5. Your mod is `build/libs/<name>-<version>.jar` (NOT the `-sources` one).
6. Put it in your `.minecraft/mods` folder together with **Fabric API**.

Alternative: use this folder as-is, but first fix the version lines in `gradle.properties`
using the numbers from the Fabric website, and run Gradle 9.x (`gradle build`).

## Needs

- Java 21
- Fabric Loader, Fabric API for 1.21.11
