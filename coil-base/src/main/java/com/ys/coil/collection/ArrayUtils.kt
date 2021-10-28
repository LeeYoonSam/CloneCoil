package com.ys.coil.collection

/** Android의 ArrayUtils 및 GrowingArrayUtils에서 채택되었습니다. */
internal object ArrayUtils {

    /**
     * 지정된 인덱스의 배열에 요소를 삽입하고 더 이상 공간이 없으면 배열을 늘립니다.
     *
     * @param array 요소를 추가할 배열입니다. null이 아니어야 합니다.
     * @param currentSize 배열의 요소 수. array.length보다 작거나 같아야 합니다.
     * @param element 삽입할 요소입니다.
     * @return 요소가 추가된 배열. 이것은 주어진 배열과 다를 수 있습니다.
     */
    fun insert(array: IntArray, currentSize: Int, index: Int, element: Int): IntArray {
        if (currentSize + 1 <= array.count()) {
            System.arraycopy(array, index, array, index + 1, currentSize - index)
            array[index] = element
            return array
        }

        val newArray = IntArray(growSize(currentSize))
        System.arraycopy(array, 0, newArray, 0, index)
        newArray[index] = element
        System.arraycopy(array, index, newArray, index + 1, array.count() - index)
        return newArray
    }

    /**
     * 배열의 현재 크기가 주어지면 배열이 확장되어야 하는 이상적인 크기를 반환합니다.
     * 이것은 일반적으로 주어진 크기의 두 배이지만 앞으로 그렇게 하도록 의존해서는 안 됩니다.
     */
    fun growSize(currentSize: Int): Int {
        return if (currentSize <= 4) 8 else currentSize * 2
    }
}