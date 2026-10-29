package com.invatech.pockit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.invatech.pockit.ui.friends.model.Friend

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val avatarEmoji: String = "👤",
    val note: String = "",
    val friendshipId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFriend(): Friend = Friend(
        id = id,
        name = name,
        email = email,
        phone = phone,
        avatarEmoji = avatarEmoji,
        note = note,
        friendshipId = friendshipId,
        createdAt = createdAt
    )

    companion object {
        fun fromFriend(friend: Friend): FriendEntity = FriendEntity(
            id = friend.id,
            name = friend.name,
            email = friend.email,
            phone = friend.phone,
            avatarEmoji = friend.avatarEmoji,
            note = friend.note,
            friendshipId = friend.friendshipId,
            createdAt = friend.createdAt
        )
    }
}
