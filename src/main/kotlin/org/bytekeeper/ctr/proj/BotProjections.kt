package org.bytekeeper.ctr.proj

import org.bytekeeper.ctr.*
import org.bytekeeper.ctr.entity.Bot
import org.springframework.stereotype.Component
import java.time.Instant
import javax.transaction.Transactional

@Component
class BotProjections(private val botService: BotService,
                     private val events: Events) {
    @Transactional
    @EventHandler
    fun onGameEnded(gameEnded: GameEnded) {
        val (botA, botB) = botService.getBotsForUpdate(listOf(gameEnded.winner, gameEnded.loser))
        botA.played++
        botB.played++
    }

    @Transactional
    @EventHandler
    fun onGameWon(gameWon: GameWon) {
        val (winner, loser) = botService.getBotsForUpdate(listOf(gameWon.winner, gameWon.loser))
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
        val (botA, botB) = botService.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
        if (event.botACrashed) botA.crashed++
        if (event.botBCrashed) botB.crashed++
    }

    @Transactional
    @EventHandler
    fun onGameFailedToStart(event: GameFailedToStart) {
        val (botA, botB) = botService.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
        botA.crashed++
        botB.crashed++
    }

    @Transactional
    @EventHandler
    fun onGameTimedOut(event: GameTimedOut) {
        val (botA, botB) = botService.getBotsForUpdate(listOf(event.botA, event.botB))
        botA.played++
        botB.played++
    }

    @Transactional
    @CommandHandler
    fun handle(command: CreateBot) {
        val bot = botService.save(Bot(name = command.name, race = command.race, botType = command.botType, lastUpdated = command.lastUpdated))
        events.post(BotCreated(bot))
    }

    @Transactional
    @EventHandler
    fun handle(botUpdated: BotUpdated) {
        val bot = botService.getById(botUpdated.bot.id!!)
        bot.lastUpdated = botUpdated.timestamp
    }

    @Transactional
    @EventHandler
    fun onBotDisabled(command: BotDisabled) {
        botService.getById(command.bot.id!!).enabled = false
    }

    @Transactional
    @EventHandler
    fun onBotEnabled(command: BotEnabled) {
        botService.getById(command.bot.id!!).enabled = true
    }
}