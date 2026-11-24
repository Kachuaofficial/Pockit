package com.invatech.pockit.data.local.entity

/*
 * Local cached space metadata. Members and space type are reconstructed through the model/converter layer.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.model.SpaceType

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val themeIndex: Int,
    val inviteCode: String,
    val members: List<Member>,
    val createdBy: String,
    val type: SpaceType = SpaceType.BILL_SPLIT,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toSpace(): Space = Space(
        id = id,
        name = name,
        emoji = emoji,
        themeIndex = themeIndex,
        inviteCode = inviteCode,
        members = members,
        createdBy = createdBy,
        type = type
    )

    companion object {
        fun fromSpace(space: Space): SpaceEntity = SpaceEntity(
            id = space.id,
            name = space.name,
            emoji = space.emoji,
            themeIndex = space.themeIndex,
            inviteCode = space.inviteCode,
            members = space.members,
            createdBy = space.createdBy,
            type = space.type
        )
    }
}
