# Contributing to `gradle-bloop`

Thanks a lot for being willing to contribute! `gradle-bloop` is a pretty minimal
sbt build. You'll find instructions down below:

## Compiling the project

To fully compile the project you can do:

```
sbt compile
```

Remember that the project is cross-compiled from 2.11-2.13. So to run any of the
commands here for all versions, remember to preface the command with `+`. So for
example to compile all versions do:

```
sbt +compile
```

## Publish the project locally

```
sbt publishLocal
```

## Testing

```
sbt test
```

## Releasing

Thanks to [sbt-ci-release](https://github.com/sbt/sbt-ci-release) the releasing
of the plugin is fully automated. Just push a new tag, and it will trigger a new
release.
