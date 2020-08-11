package io.armory.plugin.metadata

import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class PluginMetadataTest : JUnit5Minutests {
  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture()
    }

    test("insert into an empty repo; output is repo with single top-level metadata entries") {
      expectThat(emptyRepo.insert(metadata)) {
        get { size }.isEqualTo(1)
        get { first() }
          .and {
            get { id }.isEqualTo("Armory.NotificationAgent")
            get { releases.first().version }.isEqualTo("0.0.3")
          }
      }
    }

    test("insert into repo containing single plugin with different ID; output is repo with two top-level metadata entries") {
      expectThat(differentPluginRepo.insert(metadata)) {
        get { size }.isEqualTo(2)

        get { find { it.id == "Armory.NotificationAgent" } }
          .isNotNull()
          .and {
            get { releases.first().version }.isEqualTo("0.0.3")
          }

        get { find { it.id == "Armory.RandomWaitPlugin" } }
          .isNotNull()
          .and {
            get { releases.first().version }.isEqualTo("1.1.14")
          }
      }
    }

    test("insert into repo containing single plugin with same ID; output is single top-level metadata entry but two releases") {
      expectThat(samePluginRepo.insert(metadata)) {
        get { size }.isEqualTo(1)

        get { find { it.id == "Armory.NotificationAgent" } }
          .isNotNull()
          .and {
            // Sort order is as expected (newest release on top).
            get { releases[0].version }.isEqualTo("0.0.3")
            get { releases[1].version }.isEqualTo("0.0.2")
          }
      }
    }

    test("insert into repo with multiple plugins; output is repo with two top-level metadata entries, inserted plugin has two releases") {
      expectThat(compositePluginRepo.insert(metadata)) {
        get { size }.isEqualTo(2)

        get { find { it.id == "Armory.NotificationAgent" } }
          .isNotNull()
          .and {
            // Sort order is as expected (newest release on top).
            get { releases[0].version }.isEqualTo("0.0.3")
            get { releases[1].version }.isEqualTo("0.0.2")
          }

        get { find { it.id == "Armory.RandomWaitPlugin" } }
          .isNotNull()
          .and {
            get { releases.first().version }.isEqualTo("1.1.14")
          }
      }
    }
  }

  class Fixture {
    val metadata = PluginMetadata(
      id = "Armory.NotificationAgent",
      description = "An example Echo notification service",
      provider = "https://github.com/danielpeach",
      releases = mutableListOf(
        PluginRelease(
          version = "0.0.3",
          date = "2020-07-31T15:06:27.091Z",
          requires = "echo>=0.0.0",
          sha512sum = "12345",
          url = "https://github.com/spinnaker-plugin-examples/notificationPlugin/releases/download/v0.0.3/notificationPlugin-v0.0.3.zip"
        )
      )
    )

    val emptyRepo = mutableListOf<PluginMetadata>()

    val differentPluginRepo = mutableListOf(
      PluginMetadata(
        id = "Armory.RandomWaitPlugin",
        description = "An example of a PF4J-based plugin that provides a custom pipeline stage.",
        provider = "https://github.com/claymccoy",
        releases = mutableListOf(
          PluginRelease(
            version = "1.1.14",
            date = "2020-07-31T15:06:27.091Z",
            requires = "orca>=0.0.0",
            sha512sum = "12345",
            url = "https://github.com/spinnaker-plugin-examples/pf4jStagePlugin/releases/download/v1.1.14/pf4jStagePlugin-v1.1.14.zip"
          )
        )
      )
    )

    val samePluginRepo = mutableListOf(PluginMetadata(
      id = "Armory.NotificationAgent",
      description = "An example Echo notification service",
      provider = "https://github.com/danielpeach",
      releases = mutableListOf(
        PluginRelease(
          version = "0.0.2",
          date = "2020-07-30T15:06:27.091Z",
          requires = "echo>=0.0.0",
          sha512sum = "12345",
          url = "https://github.com/spinnaker-plugin-examples/notificationPlugin/releases/download/v0.0.2/notificationPlugin-v0.0.2.zip"
        )
      )
    ))

    val compositePluginRepo = mutableListOf(
      PluginMetadata(
        id = "Armory.NotificationAgent",
        description = "An example Echo notification service",
        provider = "https://github.com/danielpeach",
        releases = mutableListOf(
          PluginRelease(
            version = "0.0.2",
            date = "2020-07-30T15:06:27.091Z",
            requires = "echo>=0.0.0",
            sha512sum = "12345",
            url = "https://github.com/spinnaker-plugin-examples/notificationPlugin/releases/download/v0.0.2/notificationPlugin-v0.0.2.zip"
          )
        )
      ),
      PluginMetadata(
        id = "Armory.RandomWaitPlugin",
        description = "An example of a PF4J-based plugin that provides a custom pipeline stage.",
        provider = "https://github.com/claymccoy",
        releases = mutableListOf(
          PluginRelease(
            version = "1.1.14",
            date = "2020-07-31T15:06:27.091Z",
            requires = "orca>=0.0.0",
            sha512sum = "12345",
            url = "https://github.com/spinnaker-plugin-examples/pf4jStagePlugin/releases/download/v1.1.14/pf4jStagePlugin-v1.1.14.zip"
          )
        )
      )
    )
  }
}
