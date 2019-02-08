package org.bytekeeper.ctr.proj

import org.bytekeeper.ctr.*
import org.bytekeeper.ctr.entity.BotRepository
import org.bytekeeper.ctr.entity.getBotsForUpdate
import org.bytekeeper.ctr.math.Elo
import org.springframework.stereotype.Component
import java.time.Instant
import javax.transaction.Transactional

@Component
class BotProjections(private val botRepository: BotRepository,
                     private val events: Events) {

    @Transactional
    @EventHandler
    fun onGameEnded(gameEnded: GameEnded) {
        val (botA, botB) = botRepository.getBotsForUpdate(listOf(gameEnded.winner, gameEnded.loser))
        botA.played++
        botB.played++
    }

    @Transactional
    @EventHandler
    fun onGameWon(gameWon: GameWon) {
        val (winner, loser) = botRepository.getBotsForUpdate(listOf(gameWon.winner, gameWon.loser))
        winner.won++
        loser.lost++

        val (newWinnerRating, newLoserRating) = Elo.calculateElos(
                winner.rating, winner.played,
                loser.rating, loser.played)

        winner.rating = newWinnerRating
        loser.rating = newLoserRating

        val time = Instant.now()
        events.post(EloUpdated(winner, newWinnerRating, time, gameWon.gameHash))
        events.post(EloUpdated(loser, newLoserRating, time, gameWon.gameHash))
    }

    @Transactional
    @EventHandler
    fun onGameCrashed(event: GameCrashed) {
        val (botA, botB) = botRepository.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
        if (event.botACrashed) {
            botA.crashed++
            botA.crashesSinceUpdate++
        }
        if (event.botBCrashed) {
            botB.crashed++
            botB.crashesSinceUpdate++
        }
    }

    @Transactional
    @EventHandler
    fun onGameFailedToStart(event: GameFailedToStart) {
        val (botA, botB) = botRepository.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
        botA.crashed++
        botB.crashed++
    }

    @Transactional
    @EventHandler
    fun onGameTimedOut(event: GameTimedOut) {
        val (botA, botB) = botRepository.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
    }

    @Transactional
    @EventHandler
    fun onBotUpdated(botUpdated: BotBinaryUpdated) {
        val bot = botRepository.getById(botUpdated.bot.id!!)
        bot.lastUpdated = botUpdated.timestamp
        bot.crashesSinceUpdate = 0
    }
}