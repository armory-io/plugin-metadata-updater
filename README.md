# Metadata Repo Updater

`Metadata Repo Updater` is a GitHub action for updating a Spinnaker plugin metadata repository.

Spinnaker's plugin framework exports a [Gradle plugin](https://github.com/spinnaker/spinnaker-gradle-project/tree/master/spinnaker-extensions) for building Spinnaker plugins.

That (Gradle) plugin includes a `releaseBundle` task that generates two artifacts: a `zip` archive containing your plugin
binary in the format that Spinnaker expects, and a `plugin-info.json` file that
contains metadata about your release.

When using this action, you are responsible for uploading the `zip` to an
HTTPS URL that your Spinnaker instance can access. Our [example
plugins](https://github.com/spinnaker-plugin-examples) use GitHub releases for
this purpose.

This action accepts the HTTPS address of your uploaded `zip`, the path to your
`plugin-info.json`, and uses that information to create a PR containing a plugin release in a provided [plugin metadata
repository](https://github.com/spinnaker-plugin-examples/examplePluginRepository). 

An example workflow looks like this:

```
name: Release

on:
  push:
    tags:
    - "v*"

jobs:
  bump-dependencies:
    runs-on: ubuntu-latest
    steps:
    - name: build
      run: ./gradlew releaseBundle

    ...an upload step...
    
    - name: update metadata repo
      uses: armory-io/plugin-metadata-updater@master
      with:
        metadata: build/distributions/plugin-info.json
        binary_url: 
        metadata_repo_url: https://github.com/spinnaker-plugin-examples/examplePluginRepository
      env:
        GITHUB_OAUTH: ${{ secrets.REPO_OAUTH_TOKEN }}
```
