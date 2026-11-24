package com.invatech.pockit.ui.common.ads

/** Broad feed coverage with real content between ads and after the last ad. */
object NativePlacementPolicy {
    fun afterRow(index:Int,total:Int):Boolean =
        total>=6 && index>=4 && (index-4)%8==0 && index<total-1
}
