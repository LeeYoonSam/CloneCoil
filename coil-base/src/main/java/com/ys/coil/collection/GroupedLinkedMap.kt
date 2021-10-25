package com.ys.coil.collection

import com.ys.coil.util.orEmpty

/**
 * 개별 개체가 아닌 개체 그룹에 대한 액세스 순서라는 점을 제외하고 액세스 순서가 지정된 경우 [LinkedHashMap]과 유사합니다.
 */
internal class GroupedLinkedMap<K, V> {

    private val head = LinkedEntry<K, V>()
    private val keyToEntry = HashMap<K, LinkedEntry<K, V>>()

    operator fun set(key: K, value: V) {
        val entry = keyToEntry.getOrPut(key) {
            LinkedEntry<K, V>(key).apply(::makeTail)
        }
        entry.add(value)
    }

    operator fun get(key: K): V? {
        val entry = keyToEntry.getOrPut(key) {
            LinkedEntry(key)
        }
        makeHead(entry)
        return entry.removeLast()
    }

    fun removeLast(): V? {
        var last = head.prev

        while (last != head) {
            val removed = last.removeLast()
            if (removed != null) {
                return removed
            } else {
                // Clean up empty LRU entries since they are likely to have been one off or
                // unusual sizes and are not likely to be requested again.
                // Doing so will speed up our removeLast operation in the future and prevent our
                // linked list from growing to arbitrarily large sizes.
                removeEntry(last)
                keyToEntry.remove(last.key)
            }

            last = last.prev
        }

        return null
    }

    override fun toString() = buildString {
        append("GroupedLinkedMap( ")

        var current = head.next
        var hasAtLeastOneItem = false

        while (current != head) {
            hasAtLeastOneItem = true

            append('{')
            append(current.key)
            append(':')
            append(current.size())
            append("}, ")

            current = current.next
        }

        if (hasAtLeastOneItem) {
            delete(length - 2, length)
        }
        append(" )")
    }

    /** [entry]를 헤더 뒤로 이동 */
    private fun makeHead(entry: LinkedEntry<K, V>) {
        removeEntry(entry)
        entry.prev = head
        entry.next = head.next
        updateEntry(entry)
    }

    /** [entry]를 헤더 앞으로 이동 */
    private fun makeTail(entry: LinkedEntry<K, V>) {
        removeEntry(entry)
        entry.prev = head.prev
        entry.next = head
        updateEntry(entry)
    }

    private fun <K, V> updateEntry(entry: LinkedEntry<K, V>) {
        entry.next.prev = entry
        entry.prev.next = entry
    }

    private fun <K, V> removeEntry(entry: LinkedEntry<K, V>) {
        entry.prev.next = entry.next
        entry.next.prev = entry.prev
    }

    private class LinkedEntry<K,V>(val key: K? = null) {

        private var values: MutableList<V>? = null
        var prev: LinkedEntry<K, V> = this
        var next: LinkedEntry<K, V> = prev

        fun removeLast(): V? = values?.removeLast()

        fun size(): Int = values?.count() ?: 0

        fun add(value: V) {
            values = values.orEmpty().apply { add(value) }
        }
    }
}