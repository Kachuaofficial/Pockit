package com.invatech.pockit
import com.invatech.pockit.ui.common.ads.NativePlacementPolicy
import org.junit.Assert.*
import org.junit.Test
class NativePlacementPolicyTest {
    @Test fun noAdsInShortFeedsOrAfterLastContentRow() {
        for(size in 0..5)for(index in 0 until size)assertFalse(NativePlacementPolicy.afterRow(index,size))
        for(size in 1..100)assertFalse(NativePlacementPolicy.afterRow(size-1,size))
    }
    @Test fun longFeedsHaveBoundedSpacingAndContentOnBothSides() {
        val positions=(0 until 100).filter{NativePlacementPolicy.afterRow(it,100)}
        assertTrue(positions.isNotEmpty());assertTrue(positions.first()>=4)
        positions.zipWithNext().forEach{(a,b)->assertTrue(b-a>=8)}
        assertTrue(positions.last()<99)
    }
}
