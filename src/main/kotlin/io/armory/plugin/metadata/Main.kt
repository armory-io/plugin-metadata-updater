package io.armory.plugin.metadata

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.inputStream
import com.google.gson.GsonBuilder
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GitHubBuilder
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

class MetadataRepoUpdater : CliktCommand() {

  private val logger = KotlinLogging.logger {}

  companion object {
    const val GITHUB_OAUTH_TOKEN_ENV_NAME = "GITHUB_OAUTH"
  }

  private val mapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

  private val metadata by option(help = "plugin metadata as produced by 'gradle releaseBundle'")
    .inputStream()
    .convert { mapper.readValue<PluginMetadata>(it) }
    .required()
    .validate {
      when (it.releases.size) {
        0 -> fail("Plugin metadata does not include any releases")
        1 -> Unit
        else -> fail("Plugin metadata cannot include more than one release")
      }
    }

  private val binaryUrl by option(help = "plugin binary URL").required()

  private val metadataRepoUrl by option (help = "plugin metadata repository URL").required().validate {
    if (!it.startsWith("https://github.com/")) {
      fail("The metadata repository must be hosted on GitHub")
    }
  }

  private val oauthToken by option(envvar = GITHUB_OAUTH_TOKEN_ENV_NAME).required()

  private val tempDir = Files.createTempDirectory("plugin-metadata-").also {
    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() {
        it.toFile().deleteRecursively()
      }
    })
  }

  override fun run() {
    val (owner, repoName) = metadataRepoUrl.substring("https://github.com/".length).split("/")

    logger.info { "Cloning repo $metadataRepoUrl..."}
    val git = Git.cloneRepository()
      .setCredentialsProvider(UsernamePasswordCredentialsProvider("ignored-username", oauthToken))
      .setURI(metadataRepoUrl)
      .setDirectory(tempDir.toFile())
      .call()

    logger.info { "Reading plugin metadata..." }
    val metadataRepo = "plugins.json".resolve().inputStream().use {
      mapper.readValue<MutableList<PluginMetadata>>(it)
    }

    logger.info { "Updating plugin metadata..." }
    val updatedMetadataRepo = metadataRepo.insert(metadata.clean().withURL(binaryUrl))

    logger.info { "Writing plugin metadata updates to file..." }
    "plugins.json".resolve().outputStream().use {
      updatedMetadataRepo.write(it)
    }

    val branch = "${metadata.id}-${metadata.releases.first().version}"

    logger.info { "Commiting changes; branch is $branch..."}
    git.checkout().setName(branch).setCreateBranch(true).call()
    git.commit().setMessage("Release ${metadata.id} ${metadata.releases.first().version}").setAll(true).call()

    logger.info { "Force-pushing changes to $metadataRepoUrl" }
    git.remoteAdd().setName("userFork").setUri(URIish(metadataRepoUrl)).call()
    git.push()
      .setCredentialsProvider(UsernamePasswordCredentialsProvider("ignored-username", oauthToken))
      .setRemote("userFork")
      .setRefSpecs(RefSpec("$branch:$branch"))
      .setForce(true)
      .call()

    val github = GitHubBuilder().withOAuthToken(oauthToken).build()
    val githubRepo = github.getRepository("$owner/$repoName")

    // If there's already an existing PR, we can just reuse it... we already force-pushed the branch, so it'll
    // automatically update.
    val existingPr = githubRepo.getPullRequests(GHIssueState.OPEN)
      .firstOrNull { pr -> pr.labels.map { label -> label.name }.contains(branch) }

    if (existingPr != null) {
      logger.info { "Found existing PR for repo $metadataRepoUrl: ${existingPr.htmlUrl}" }
      return
    }

    logger.info { "Creating pull request for repo $metadataRepoUrl" }
    val pr = githubRepo.createPullRequest(
      /* title= */"Release ${metadata.id} ${metadata.releases.first().version}",
      /* head= */ "$owner:$branch",
      /* base= */ githubRepo.defaultBranch,
      /* body= */ ""
    )

    pr.addLabels(branch)
    logger.info { "Created pull request for $metadataRepoUrl: ${pr.htmlUrl}" }
  }

  private fun String.resolve() = File(tempDir.resolve(this).toUri())
}

internal fun MutableList<PluginMetadata>.write(stream: OutputStream) =
  // Jackson doesn't pretty-print JSON very well.
  GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(this).let {
    stream.write((it + "\n").toByteArray())
  }

internal fun MutableList<PluginMetadata>.insert(metadata: PluginMetadata): MutableList<PluginMetadata> {
  if (any { it.id == metadata.id }) {
    find { it.id == metadata.id }?.releases?.add(0, metadata.releases.first())
  } else {
    add(metadata)
  }
  return this
}

internal fun PluginMetadata.clean() = copy(
  releases = releases.map { it.copy(version = if (it.version.startsWith("v")) { it.version.substring(1) } else { it.version }) }.toMutableList()
)

internal fun PluginMetadata.withURL(url: String) = copy(
  releases = mutableListOf(releases.first().copy(url = url))
)

data class PluginMetadata(
  val id: String,
  val description: String,
  val provider: String,
  val releases: MutableList<PluginRelease>
)

data class PluginRelease(
  val version: String,
  val date: String,
  val requires: String,
  val sha512sum: String,
  val state: String? = null,
  val url: String = ""
)

fun main(args: Array<String>) {
  MetadataRepoUpdater().main(args)
}
