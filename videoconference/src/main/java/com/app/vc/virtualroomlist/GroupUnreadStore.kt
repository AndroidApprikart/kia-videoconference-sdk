package com.app.vc.virtualroomlist

object GroupUnreadStore {
    private val unreadCounts = linkedMapOf<String, Int>()
    private val rawUnreadCounts = linkedMapOf<String, Int>()
    private val readBaselines = linkedMapOf<String, Int>()
    private val listeners = linkedSetOf<(Map<String, Int>) -> Unit>()

    fun replaceAll(counts: Map<String, Int>) {
        val validGroups = counts.keys
        rawUnreadCounts.keys.retainAll(validGroups)
        readBaselines.keys.retainAll(validGroups)
        unreadCounts.clear()
        counts.forEach { (slug, count) ->
            val rawCount = count.coerceAtLeast(0)
            rawUnreadCounts[slug] = rawCount
            unreadCounts[slug] = computeVisibleUnreadCount(slug, rawCount)
        }
        notifyListeners()
    }

    fun updateUnreadCount(groupSlug: String, unreadCount: Int) {
        if (groupSlug.isBlank()) return
        val rawCount = unreadCount.coerceAtLeast(0)
        rawUnreadCounts[groupSlug] = rawCount
        unreadCounts[groupSlug] = computeVisibleUnreadCount(groupSlug, rawCount)
        notifyListeners()
    }

    fun markRead(groupSlug: String) {
        if (groupSlug.isBlank()) return
        readBaselines[groupSlug] = rawUnreadCounts[groupSlug] ?: unreadCounts[groupSlug] ?: 0
        unreadCounts[groupSlug] = 0
        notifyListeners()
    }

    fun getUnreadCount(groupSlug: String): Int = unreadCounts[groupSlug] ?: 0

    fun snapshot(): Map<String, Int> = unreadCounts.toMap()

    fun clear() {
        unreadCounts.clear()
        rawUnreadCounts.clear()
        readBaselines.clear()
        notifyListeners()
    }

    fun addListener(listener: (Map<String, Int>) -> Unit) {
        listeners.add(listener)
        listener(unreadCounts.toMap())
    }

    fun removeListener(listener: (Map<String, Int>) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        val snapshot = unreadCounts.toMap()
        listeners.forEach { it(snapshot) }
    }

    private fun computeVisibleUnreadCount(groupSlug: String, rawCount: Int): Int {
        if (rawCount <= 0) {
            readBaselines.remove(groupSlug)
            return 0
        }
        val baseline = readBaselines[groupSlug] ?: return rawCount
        return (rawCount - baseline).coerceAtLeast(0)
    }
}
