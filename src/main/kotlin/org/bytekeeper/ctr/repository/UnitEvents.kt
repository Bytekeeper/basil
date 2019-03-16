package org.bytekeeper.ctr.repository

import io.micrometer.core.annotation.Timed
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import javax.persistence.*


enum class UnitEventType {
    INVALID,
    UNIT_CREATE,
    UNIT_COMPLETE,
    UNIT_SHOW,
    UNIT_HIDE,
    UNIT_RENEGADE,
    UNIT_DESTROY,
    UNIT_MORPH;

    companion object {
        fun fromLogEvent(event: String) = when (event) {
            "unitCreate" -> UNIT_CREATE
            "unitComplete" -> UNIT_COMPLETE
            "unitShow" -> UNIT_SHOW
            "unitHide" -> UNIT_HIDE
            "unitRenegade" -> UNIT_RENEGADE
            "unitDestroy" -> UNIT_DESTROY
            "unitMorph" -> UNIT_MORPH
            else -> INVALID
        }
    }
}

@Entity
class UnitEvent(val frame: Int,
                @Enumerated(EnumType.STRING) val event: UnitEventType,
                @ManyToOne(fetch = FetchType.LAZY) val game: GameResult,
                @ManyToOne(fetch = FetchType.LAZY) val bot: Bot,
                val ownedByBot: Boolean,
                val unitId: Int,
                val unitType: String,
                val posX: Int,
                val posY: Int) {
    @Id
    @GeneratedValue
    var id: Long? = null
}

data class UnitStats(val name: String, val event: UnitEventType, val amount: Long)

interface UnitEventsRepository : CrudRepository<UnitEvent, Long> {
    @Query("SELECT new org.bytekeeper.ctr.repository.UnitStats(unitType, event, count(*))" +
            " FROM UnitEvent GROUP BY unitType, event")
    @Timed
    fun globalUnitStats(): List<UnitStats>

}