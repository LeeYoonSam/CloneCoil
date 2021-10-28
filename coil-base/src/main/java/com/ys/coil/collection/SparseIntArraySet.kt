package com.ys.coil.collection

import android.util.SparseIntArray

/**
 * 순서가 지정되지 않은 고유한 [Int] 모음입니다. [Int]는 [Array]에 프리미티브로 저장되어 메모리 사용량을 줄입니다.
 *
 * @see SparseIntArray
 */
class SparseIntArraySet(initialCapacity: Int = 10) {

    private var elements = IntArray(initialCapacity)
    private var size = 0

    /**
     * 집합에 요소를 추가합니다.
     */
    fun add(element: Int): Boolean {
        val i = indexOfElement(element)
        val absent = i < 0
        if (absent) {
            elements = ArrayUtils.insert(elements, size, i.inv(), element)
            size++
        }
        return absent
    }

    /**
     * 집합에서 요소를 제거합니다. 존재하는 경우 true를 반환합니다.
     */
    fun remove(element: Int): Boolean {
        val i = indexOfElement(element)
        val present = i >= 0
        if (present) {
            removeAt(i)
        }
        return present
    }

    /**
     * SparseIntArraySet에 이 요소가 포함되어 있으면 true를 반환합니다.
     */
    fun contains(element: Int): Boolean = elements.binarySearch(element, toIndex = size) >= 0

    /**
     * 주어진 인덱스에서 요소를 제거합니다.
     */
    fun removeAt(index: Int) {
        System.arraycopy(elements, index + 1, elements, index, size - (index + 1))
        size--
    }

    /**
     * 이 SparseIntArraySet이 현재 저장하고 있는 요소의 수를 반환합니다.
     */
    fun size(): Int = size

    /**
     * '[0, size)' 범위의 인덱스가 주어지면 이 SparseIntArraySet이 저장하는 '인덱스' 번째 키-값 매핑에서 요소를 반환합니다.
     *
     * 오름차순의 인덱스에 해당하는 요소는 오름차순으로 보장됩니다.
     * 예를 들어 'elementAt(0)'은 가장 작은 요소를 반환하고 'elementAt(size()-1)'은 가장 큰 요소를 반환합니다.
     */
    fun elementAt(index: Int): Int = elements[index]

    /**
     * [elementAt]가 지정된 요소를 반환하는 인덱스를 반환하거나 지정된 요소가 매핑되지 않은 경우 음수를 반환합니다.
     */
    fun indexOfElement(key: Int): Int = elements.binarySearch(key, toIndex = size)

    /**
     * Removes all elements from this SparseIntArraySet.
     */
    fun clear() {
        size = 0
    }
}