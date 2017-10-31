# sbt-macos-watcher

Hotfix for lethargic watchService on SBT 1.0+ MacOS.

## Usage

Edit `~/.sbt/1.0/plugins/plugins.sbt` and add the following lines:

```
resolvers += Resolver.bintrayIvyRepo( "megri", "sbt-plugins" )
addSbtPlugin( "com.megri" % "sbt-macos-watcher" % "0.1" )
```

The plugin should automatically replace the defauld sbt watchservice with a native one.

## Disclaimer

Things may not work as intended for your use-case. Feel free to create an issue.

## Legal

License is transitive on https://github.com/gjoseph/BarbaryWatchService, which borrows code from OpenJDK 7 "as much as possible". I'm no lawyer but I believe GPL 2.0-CE should work. If this is incorrect and you care about licensing, please add an issue so I can correct it.

