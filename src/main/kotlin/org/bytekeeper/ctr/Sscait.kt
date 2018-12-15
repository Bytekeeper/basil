package org.bytekeeper.ctr

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.bytekeeper.ctr.entity.Race
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class SscaitSource : BotSource {
    private val webClient = WebClient.builder().baseUrl("https://sscaitournament.com/").build()

    private var botCache: Map<String, org.bytekeeper.ctr.BotInfo> = emptyMap()
    override fun allBotInfos(): List<org.bytekeeper.ctr.BotInfo> = botCache.values.toList()

    override fun refresh() {
        botCache = webClient.get().uri("api/bots.php")
                .retrieve()
                .bodyToFlux<BotInfo>()
                .collectList()
                .block()!!
                .map { it.name to it }.toMap()
    }

    override fun downloadBwapiDLL(info: org.bytekeeper.ctr.BotInfo): InputStream? {
        return (info as? BotInfo)?.let { botInfo ->
            webClient.get().uri(botInfo.bwapiDLL)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono<InputStream>()
                    .block(Duration.ofSeconds(10))
                    ?: throw FailedToDownloadBwApi("Could not download BWAPI.dll for bot ${botInfo.name}")
        }
    }

    override fun downloadBinary(info: org.bytekeeper.ctr.BotInfo): InputStream? {
        return (info as? BotInfo)?.let { botInfo ->
            webClient.get().uri(botInfo.botBinary)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono<InputStream>()
                    .block(Duration.ofSeconds(30))
                    ?: throw FailedToDownloadBot("Could not download bot binary for bot ${botInfo.name}")
        }
    }

    companion object {
        val SSCAIT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    }

    class BotInfo(
            override val name: String,
            @JsonProperty("race")
            val raceValue: String,
            val wins: String? = "0",
            val losses: String? = "0",
            val status: String? = "Disabled",
            private val update: String? = null,
            val botBinary: String,
            val bwapiDLL: String,
            override val botType: String,
            val description: String? = ""
    ) : org.bytekeeper.ctr.BotInfo {
        private val basilCommands: List<String>
            get() {
                val matcher = BASIL_COMMAND_MATCHER.matcher(description)
                if (matcher.find()) {
                    return commands(matcher.group(1))
                }
                return emptyList()
            }

        override val disabled = status == "Disabled" || basilCommands.contains("DISABLED")

        override val clearReadDirectory = basilCommands.contains("RESET")

        override val publishReadDirectory = basilCommands.contains("PUBLISH-READ")

        override fun lastUpdated(): Instant =
                if (update == null) Instant.MIN else LocalDateTime.parse(update, SSCAIT_DATE_FORMAT).toInstant(ZoneOffset.UTC)

        @JsonIgnore
        override val race: Race = parseRace(raceValue)

        private fun parseRace(race: String): Race =
                when (race) {
                    "Terran" -> Race.TERRAN
                    "Zerg" -> Race.ZERG
                    "Protoss" -> Race.PROTOSS
                    "Random" -> Race.RANDOM
                    else -> throw IllegalStateException("Unknown race $race")
                }

        companion object {
            internal val BASIL_COMMAND_MATCHER = "BASIL:\\s*((?:\\S+\\s*,\\s*)*\\S+)".toPattern()
            internal fun commands(basicCommand: String) = basicCommand.split("\\s*,\\s*".toRegex())
        }
    }

}
