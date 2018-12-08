package org.bytekeeper.ctr

import org.bytekeeper.ctr.entity.Bot
import org.bytekeeper.ctr.entity.BotRepository
import org.springframework.stereotype.Service

@Service
class BotService(val botRepository: BotRepository) {
    fun getBotsForUpdate(bots: List<Bot>) =
            bots.mapIndexed { index, bot -> index to bot }
                    .sortedBy { it.second.name }
                    .map {
                        it.first to botRepository.getById(it.second.id!!)
                    }
                    .sortedBy { it.first }
                    .map { it.second }

    fun save(bot: Bot) = botRepository.save(bot)
}