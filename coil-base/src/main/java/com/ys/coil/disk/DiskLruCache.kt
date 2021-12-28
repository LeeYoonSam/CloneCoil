/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ys.coil.disk

import com.ys.coil.disk.DiskLruCache.Editor
import com.ys.coil.util.deleteContents
import com.ys.coil.util.deleteIfExists
import com.ys.coil.util.forEachIndices
import okio.BufferedSink
import okio.Closeable
import okio.EOFException
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Sink
import okio.blackholeSink
import okio.buffer
import java.io.File
import java.util.concurrent.Executors

/**
 * 파일 시스템에서 제한된 양의 공간을 사용하는 캐시입니다.
 * 각 캐시 항목에는 문자열 키와 고정된 수의 값이 있습니다.
 * 각 키는 정규식 `[a-z0-9_-]{1,64}`과 일치해야 합니다.
 * 값은 스트림 또는 파일로 액세스할 수 있는 바이트 시퀀스입니다.
 * 각 값의 길이는 '0'에서 'Int.MAX_VALUE'바이트 사이여야 합니다.
 *
 * 캐시는 파일 시스템의 디렉토리에 데이터를 저장합니다.
 * 이 디렉토리는 캐시 전용이어야 합니다.
 * 캐시는 디렉토리에서 파일을 삭제하거나 덮어쓸 수 있습니다.
 * 여러 프로세스가 동시에 동일한 캐시 디렉토리를 사용하는 것은 오류입니다.
 *
 * 이 캐시는 파일 시스템에 저장할 바이트 수를 제한합니다.
 * 저장된 바이트 수가 제한을 초과하면 캐시는 제한이 충족될 때까지 백그라운드에서 항목을 제거합니다.
 * 제한은 엄격하지 않습니다.
 * 파일이 삭제되기를 기다리는 동안 캐시가 일시적으로 초과할 수 있습니다.
 * 이 제한에는 파일 시스템 오버헤드나 캐시 저널이 포함되지 않으므로 공간에 민감한 애플리케이션은 보수적 제한을 설정해야 합니다.
 *
 * 클라이언트는 항목의 값을 생성하거나 업데이트하기 위해 [edit]를 호출합니다.
 * 항목에는 한 번에 하나의 편집기만 있을 수 있습니다.
 * 값을 편집할 수 없는 경우 [edit]은 null을 반환합니다.
 *
 *  * 항목이 **만들어질 때** 전체 값 집합을 제공해야 합니다. 필요한 경우 빈 값을 자리 표시자로 사용해야 합니다.
 *  * 항목을 **편집**할 때 모든 값에 대한 데이터를 제공할 필요는 없습니다. 값의 기본값은 이전 값입니다.
 *
 * 모든 [edit] 호출은 [Editor.commit] 또는 [Editor.abort] 호출과 일치해야 합니다. 커밋은 원자적입니다.
 * 읽기는 커밋 전후의 전체 값 집합을 관찰하지만 값의 혼합은 절대 관찰하지 않습니다.
 *
 * 클라이언트는 항목의 스냅샷을 읽기 위해 [get]을 호출합니다. 읽기는 당시의 값을 관찰할 것입니다.
 * [get]이 호출되었습니다. 호출 후 업데이트 및 제거는 진행 중인 읽기에 영향을 미치지 않습니다.
 *
 * 이 클래스는 일부 I/O 오류를 허용합니다. 파일 시스템에서 파일이 누락된 경우 해당 항목이 캐시에서 삭제됩니다.
 * 캐시 값을 쓰는 동안 오류가 발생하면 편집이 자동으로 실패합니다.
 * 호출자는 `IOException`을 포착하고 적절하게 응답하여 다른 문제를 처리해야 합니다.
 *
 * @constructor [directory]에 상주할 캐시를 만듭니다. 이 캐시는 처음 액세스할 때 느리게 초기화되며 존재하지 않는 경우 생성됩니다.
 * @param directory 쓰기 가능한 디렉토리.
 * @param valueCount 캐시 항목당 값의 수입니다. 긍정적이어야 합니다.
 * @param maxSize 이 캐시가 저장하는 데 사용해야 하는 최대 바이트 수입니다.
 */
@OptIn(ExperimentalFileSystem::class)
internal class DiskLruCache(
    fileSystem: FileSystem,
    private val directory: Path,
    private val maxSize: Long,
    private val appVersion: Int,
    private val valueCount: Int
) : Closeable {

    /*
     * 이 캐시는 "journal"이라는 저널 파일을 사용합니다. 일반적인 저널 파일은 다음과 같습니다.:
     *
     *     libcore.io.DiskLruCache
     *     1
     *     100
     *     2
     *
     *     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
     *     DIRTY 335c4c6028171cfddfbaae1a9c313c52
     *     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
     *     REMOVE 335c4c6028171cfddfbaae1a9c313c52
     *     DIRTY 1ab96a171faeeee38496d8b330771a7a
     *     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
     *     READ 335c4c6028171cfddfbaae1a9c313c52
     *     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
     *
     * 저널의 처음 5줄은 헤더를 형성합니다.
     * 상수 문자열 "libcore.io.DiskLruCache", 디스크 캐시 버전, 응용 프로그램 버전, 값 개수 및 빈 줄입니다.
     *
     * 파일의 각 후속 행은 캐시 항목의 상태에 대한 레코드입니다.
     * 각 줄에는 상태, 키 및 선택적 상태별 값과 같은 공백으로 구분된 값이 포함됩니다.
     *
     *   o DIRTY 행은 항목이 활발하게 생성 또는 업데이트되고 있음을 추적합니다.
     *      모든 성공적인 DIRTY 작업 뒤에는 CLEAN 또는 REMOVE 작업이 따라야 합니다.
     *      일치하는 CLEAN 또는 REMOVE가 없는 DIRTY 행은 임시 파일을 삭제해야 할 수도 있음을 나타냅니다.
     *
     *   o CLEAN 라인은 성공적으로 게시되어 읽을 수 있는 캐시 항목을 추적합니다. 게시 행 다음에 각 값의 길이가 옵니다.
     *
     *   o READ 라인은 LRU에 대한 액세스를 추적합니다.
     *
     *   o REMOVE 행은 삭제된 항목을 추적합니다.
     *
     * 캐시 작업이 발생하면 저널 파일이 추가됩니다.
     * 저널은 때때로 중복 행을 삭제하여 압축될 수 있습니다.
     * 압축하는 동안 "journal.tmp"라는 임시 파일이 사용됩니다.
     * 캐시가 열릴 때 해당 파일이 있으면 삭제해야 합니다.
     */

    private val journalFile: Path
    private val journalFileTmp: Path
    private val journalFileBackup: Path
    private val lruEntries = LinkedHashMap<String, Entry>(0, 0.75f, true)
    private var size = 0L
    private var redundantOpCount = 0
    private var journalWriter: BufferedSink? = null
    private var hasJournalErrors = false

    // 'this'에 동기화될 때 읽고 써야 합니다.
    private var initialized = false
    private var closed = false
    private var mostRecentTrimFailed = false
    private var mostRecentRebuildFailed = false

    // TODO: Replace with https://github.com/Kotlin/kotlinx.coroutines/issues/2919.
    private val cleanupExecutor = Executors.newSingleThreadExecutor {
        Thread().apply { name = "coil.disk.DiskLruCache: $directory" }
    }
    private val cleanupTask = Runnable {
        synchronized(this@DiskLruCache) {
            if (!initialized || closed) return@Runnable
            try {
                trimToSize()
            } catch (_: IOException) {
                mostRecentTrimFailed = true
            }
            try {
                if (journalRebuildRequired()) {
                    rebuildJournal()
                    redundantOpCount = 0
                }
            } catch (_: IOException) {
                mostRecentRebuildFailed = true
                journalWriter = blackholeSink().buffer()
            }
        }
    }

    private val fileSystem = object : ForwardingFileSystem(fileSystem) {
        override fun sink(file: Path): Sink {
            // Ensure the parent directory for the file is created if it doesn't already exist.
            file.parent?.let { if (!exists(it)) createDirectories(it) }
            return super.sink(file)
        }
    }

    init {
        require(maxSize > 0L) { "maxSize <= 0" }
        require(valueCount > 0) { "valueCount <= 0" }

        journalFile = directory / JOURNAL_FILE
        journalFileTmp = directory / JOURNAL_FILE_TEMP
        journalFileBackup = directory / JOURNAL_FILE_BACKUP
    }

    @Synchronized
    private fun initialize() {
        if (initialized) return

        // bkp 파일이 있으면 대신 사용하십시오.
        if (fileSystem.exists(journalFileBackup)) {
            // If journal file also exists just delete backup file.
            if (fileSystem.exists(journalFile)) {
                fileSystem.delete(journalFileBackup)
            } else {
                fileSystem.atomicMove(journalFileBackup, journalFile)
            }
        }

        // 우리가 중단 한 곳에서 픽업하는 것을 선호합니다.
        if (fileSystem.exists(journalFile)) {
            try {
                readJournal()
                processJournal()
                initialized = true
                return
            } catch (_: IOException) {
                // 저널이 손상되었습니다.
            }

            // 캐시가 손상되었습니다. 디렉토리의 내용을 삭제하십시오.
            // 심각한 파일 시스템 문제가 있음을 의미할 수 있으므로 이 문제가 발생할 수 있습니다.
            try {
                delete()
            } finally {
                closed = false
            }
        }

        rebuildJournal()
        initialized = true
    }

    private fun readJournal() {
        fileSystem.read(journalFile) {
            val magic = readUtf8LineStrict()
            val version = readUtf8LineStrict()
            val appVersionString = readUtf8LineStrict()
            val valueCountString = readUtf8LineStrict()
            val blank = readUtf8LineStrict()

            if (MAGIC != magic ||
                VERSION != version ||
                appVersion.toString() != appVersionString ||
                valueCount.toString() != valueCountString ||
                blank.isNotEmpty()
            ) {
                throw IOException("unexpected journal header: " +
                    "[$magic, $version, $appVersionString, $valueCountString, $blank]")
            }

            var lineCount = 0
            while (true) {
                try {
                    readJournalLine(readUtf8LineStrict())
                    lineCount++
                } catch (_: EOFException) {
                    break // End of journal.
                }
            }

            redundantOpCount = lineCount - lruEntries.size

            // 잘린 줄로 끝나면 추가하기 전에 저널을 다시 작성하십시오.
            if (!exhausted()) {
                rebuildJournal()
            } else {
                journalWriter = newJournalWriter()
            }
        }
    }

    private fun newJournalWriter(): BufferedSink {
        val fileSink = fileSystem.appendingSink(journalFile)
        val faultHidingSink = FaultHidingSink(fileSink) {
            hasJournalErrors = true
        }
        return faultHidingSink.buffer()
    }

    private fun readJournalLine(line: String) {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) throw IOException("unexpected journal line: $line")

        val keyBegin = firstSpace + 1
        val secondSpace = line.indexOf(' ', keyBegin)
        val key: String
        if (secondSpace == -1) {
            key = line.substring(keyBegin)
            if (firstSpace == REMOVE.length && line.startsWith(REMOVE)) {
                lruEntries.remove(key)
                return
            }
        } else {
            key = line.substring(keyBegin, secondSpace)
        }

        val entry = lruEntries.getOrPut(key) { Entry(key) }
        when {
            secondSpace != -1 && firstSpace == CLEAN.length && line.startsWith(CLEAN) -> {
                val parts = line.substring(secondSpace + 1).split(' ')
                entry.readable = true
                entry.currentEditor = null
                entry.setLengths(parts)
            }
            secondSpace == -1 && firstSpace == DIRTY.length && line.startsWith(DIRTY) -> {
                entry.currentEditor = Editor(entry)
            }
            secondSpace == -1 && firstSpace == READ.length && line.startsWith(READ) -> {
                // This work was already done by calling lruEntries.get().
            }
            else -> throw IOException("unexpected journal line: $line")
        }
    }

    /**
     * 초기 크기를 계산하고 캐시 열기의 일부로 가비지를 수집합니다. 더러운 항목은 일관성이 없는 것으로 간주되어 삭제됩니다.
     */
    private fun processJournal() {
        fileSystem.deleteIfExists(journalFileTmp)
        val i = lruEntries.values.iterator()
        while (i.hasNext()) {
            val entry = i.next()
            if (entry.currentEditor == null) {
                for (t in 0 until valueCount) {
                    size += entry.lengths[t]
                }
            } else {
                entry.currentEditor = null
                for (t in 0 until valueCount) {
                    fileSystem.deleteIfExists(entry.cleanFiles[t])
                    fileSystem.deleteIfExists(entry.dirtyFiles[t])
                }
                i.remove()
            }
        }
    }

    /**
     * 중복 정보를 생략하는 새 저널을 만듭니다.
     * 현재 저널이 있는 경우 이를 대체합니다.
     */
    @Synchronized
    private fun rebuildJournal() {
        journalWriter?.close()

        fileSystem.write(journalFileTmp) {
            writeUtf8(MAGIC).writeByte('\n'.code)
            writeUtf8(VERSION).writeByte('\n'.code)
            writeDecimalLong(appVersion.toLong()).writeByte('\n'.code)
            writeDecimalLong(valueCount.toLong()).writeByte('\n'.code)
            writeByte('\n'.code)

            for (entry in lruEntries.values) {
                if (entry.currentEditor != null) {
                    writeUtf8(DIRTY).writeByte(' '.code)
                    writeUtf8(entry.key)
                    writeByte('\n'.code)
                } else {
                    writeUtf8(CLEAN).writeByte(' '.code)
                    writeUtf8(entry.key)
                    entry.writeLengths(this)
                    writeByte('\n'.code)
                }
            }
        }

        if (fileSystem.exists(journalFile)) {
            fileSystem.atomicMove(journalFile, journalFileBackup)
            fileSystem.atomicMove(journalFileTmp, journalFile)
            fileSystem.deleteIfExists(journalFileBackup)
        } else {
            fileSystem.atomicMove(journalFileTmp, journalFile)
        }

        journalWriter = newJournalWriter()
        hasJournalErrors = false
        mostRecentRebuildFailed = false
    }

    /**
     * 이름이 [key]인 항목의 스냅샷을 반환하거나 현재 읽을 수 없는 항목이 없으면 null을 반환합니다.
     * 값이 반환되면 LRU 대기열의 헤드로 이동합니다.
     */
    @Synchronized
    operator fun get(key: String): Snapshot? {
        initialize()
        checkNotClosed()
        validateKey(key)

        val snapshot = lruEntries[key]?.snapshot() ?: return null

        redundantOpCount++
        journalWriter!!.apply {
            writeUtf8(READ)
            writeByte(' '.code)
            writeUtf8(key)
            writeByte('\n'.code)
        }
        if (journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }

        return snapshot
    }

    /** [key]라는 항목에 대한 편집기를 반환하거나 다른 편집이 진행 중인 경우 null을 반환합니다. */
    @Synchronized
    fun edit(key: String): Editor? {
        initialize()
        checkNotClosed()
        validateKey(key)

        var entry = lruEntries[key]

        if (entry?.currentEditor != null) {
            return null // Another edit is in progress.
        }

        if (entry != null && entry.lockingSnapshotCount != 0) {
            return null // We can't write this file because a reader is still reading it.
        }

        if (mostRecentTrimFailed || mostRecentRebuildFailed) {
            /*
             OS가 우리의 적이 되었습니다! 자르기 작업이 실패하면 사용자가 요청한 것보다 더 많은 데이터를 저장하고 있음을 의미합니다.
             더 이상 해당 제한을 초과하지 않도록 편집을 허용하지 마십시오.
             저널 재구축에 실패하면 저널 작성자가 활성화되지 않습니다.
             즉, 편집 내용을 기록할 수 없어 파일 누수가 발생합니다.
             두 경우 모두 정리를 다시 시도하여 이 상태에서 벗어날 수 있습니다!
             */
            cleanupExecutor.submit(cleanupTask)
            return null
        }

        // 파일 누출을 방지하기 위해 파일을 생성하기 전에 저널을 플러시하십시오.
        journalWriter!!.apply {
            writeUtf8(DIRTY)
            writeByte(' '.code)
            writeUtf8(key)
            writeByte('\n'.code)
            flush()
        }

        if (hasJournalErrors) {
            return null // Don't edit; the journal can't be written.
        }

        if (entry == null) {
            entry = Entry(key)
            lruEntries[key] = entry
        }
        val editor = Editor(entry)
        entry.currentEditor = editor
        return editor
    }

    /**
     * 이 캐시에 값을 저장하는 데 현재 사용 중인 바이트 수를 반환합니다.
     * 배경 삭제가 보류 중인 경우 최대 크기보다 클 수 있습니다.
     */
    @Synchronized
    fun size(): Long {
        initialize()
        return size
    }

    @Synchronized
    private fun completeEdit(editor: Editor, success: Boolean) {
        val entry = editor.entry
        check(entry.currentEditor == editor)

        for (i in 0 until valueCount) {
            val dirty = entry.dirtyFiles[i]
            if (success && !entry.zombie) {
                if (fileSystem.exists(dirty)) {
                    val clean = entry.cleanFiles[i]
                    fileSystem.atomicMove(dirty, clean)
                    val oldLength = entry.lengths[i]
                    val newLength = fileSystem.metadata(clean).size ?: 0
                    entry.lengths[i] = newLength
                    size = size - oldLength + newLength
                }
            } else {
                fileSystem.deleteIfExists(dirty)
            }
        }

        // 모든 항목이 완료되었는지 확인합니다.
        if (success) {
            for (i in 0 until valueCount) {
                entry.cleanFiles[i].toFile().createNewFile()
            }
        }

        entry.currentEditor = null
        if (entry.zombie) {
            removeEntry(entry)
            return
        }

        redundantOpCount++
        journalWriter!!.apply {
            if (entry.readable || success) {
                entry.readable = true
                writeUtf8(CLEAN).writeByte(' '.code)
                writeUtf8(entry.key)
                entry.writeLengths(this)
                writeByte('\n'.code)
            } else {
                lruEntries.remove(entry.key)
                writeUtf8(REMOVE).writeByte(' '.code)
                writeUtf8(entry.key)
                writeByte('\n'.code)
            }
            flush()
        }

        if (size > maxSize || journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }
    }

    /**
     * 저널 크기가 절반으로 줄어들고 최소 2000개의 작업을 제거할 때만 저널을 다시 작성합니다.
     */
    private fun journalRebuildRequired(): Boolean {
        return redundantOpCount >= 2000 && redundantOpCount >= lruEntries.size
    }

    /**
     * [key] 항목이 존재하고 제거할 수 있는 경우 삭제합니다.
     * [key] 항목이 현재 편집 중인 경우 해당 편집은 정상적으로 완료되지만 해당 값은 저장되지 않습니다.
     *
     * @return 항목이 제거된 경우 true입니다.
     */
    @Synchronized
    fun remove(key: String): Boolean {
        initialize()
        checkNotClosed()
        validateKey(key)

        val entry = lruEntries[key] ?: return false
        val removed = removeEntry(entry)
        if (removed && size <= maxSize) mostRecentTrimFailed = false
        return removed
    }

    private fun removeEntry(entry: Entry): Boolean {
        // 아직 열려 있는 파일을 삭제할 수 없는 경우 이 항목을 좀비로 표시하여 해당 파일이 닫힐 때 파일이 삭제되도록 합니다.
        if (entry.lockingSnapshotCount > 0) {
            // 프로세스가 충돌하는 경우 이 항목이 사용되지 않도록 이 항목을 'DIRTY'로 표시합니다.
            journalWriter?.apply {
                writeUtf8(DIRTY)
                writeByte(' '.code)
                writeUtf8(entry.key)
                writeByte('\n'.code)
                flush()
            }
        }
        if (entry.lockingSnapshotCount > 0 || entry.currentEditor != null) {
            entry.zombie = true
            return true
        }

        // 편집이 정상적으로 완료되지 않도록 합니다.
        entry.currentEditor?.detach()

        for (i in 0 until valueCount) {
            fileSystem.deleteIfExists(entry.cleanFiles[i])
            size -= entry.lengths[i]
            entry.lengths[i] = 0
        }

        redundantOpCount++
        journalWriter?.apply {
            writeUtf8(REMOVE)
            writeByte(' '.code)
            writeUtf8(entry.key)
            writeByte('\n'.code)
        }
        lruEntries.remove(entry.key)

        if (journalRebuildRequired()) {
            cleanupExecutor.submit(cleanupTask)
        }

        return true
    }

    private fun checkNotClosed() {
        check(!closed) { "cache is closed" }
    }

    /** 이 캐시를 닫습니다. 저장된 값은 파일 시스템에 남아 있습니다. */
    @Synchronized
    override fun close() {
        if (!initialized || closed) {
            closed = true
            return
        }

        // 동시 반복을 위해 복사합니다.
        for (entry in lruEntries.values.toTypedArray()) {
            if (entry.currentEditor != null) {
                // 편집이 정상적으로 완료되지 않도록 합니다.
                entry.currentEditor?.detach()
            }
        }

        trimToSize()
        journalWriter!!.close()
        journalWriter = null
        closed = true
    }

    private fun trimToSize() {
        while (size > maxSize) {
            if (!removeOldestEntry()) return
        }
        mostRecentTrimFailed = false
    }

    /** 항목이 제거된 경우 true를 반환합니다. 모든 항목이 좀비인 경우 false를 반환합니다. */
    private fun removeOldestEntry(): Boolean {
        for (toEvict in lruEntries.values) {
            if (!toEvict.zombie) {
                removeEntry(toEvict)
                return true
            }
        }
        return false
    }

    /**
     * 캐시를 닫고 저장된 모든 값을 삭제합니다. 이렇게 하면 캐시에서 생성되지 않은 파일을 포함하여 캐시 디렉터리의 모든 파일이 삭제됩니다.
     */
    fun delete() {
        close()
        fileSystem.deleteContents(directory)
    }

    /**
     * 캐시에서 저장된 모든 값을 삭제합니다. 기내 편집은 정상적으로 완료되지만 해당 값은 저장되지 않습니다.
     */
    @Synchronized
    fun evictAll() {
        initialize()
        // 동시 반복을 위해 복사합니다.
        for (entry in lruEntries.values.toTypedArray()) {
            removeEntry(entry)
        }
        mostRecentTrimFailed = false
    }

    private fun validateKey(key: String) {
        require(LEGAL_KEY_PATTERN matches key) {
            "keys must match regex [a-z0-9_-]{1,120}: \"$key\""
        }
    }

    /** 항목 값의 스냅샷입니다. */
    inner class Snapshot(private val entry: Entry) : Closeable {

        private var closed = false

        fun file(index: Int): File {
            check(!closed) { "snapshot is closed" }
            return entry.cleanFiles[index].toFile()
        }

        override fun close() {
            if (!closed) {
                closed = true
                synchronized(this@DiskLruCache) {
                    entry.lockingSnapshotCount--
                    if (entry.lockingSnapshotCount == 0 && entry.zombie) {
                        removeEntry(entry)
                    }
                }
            }
        }

        fun closeAndEdit(): Editor? {
            synchronized(this@DiskLruCache) {
                close()
                return edit(entry.key)
            }
        }
    }

    /** 항목의 값을 편집합니다. */
    inner class Editor(val entry: Entry) {

        private var closed = false

        /**
         * [index]에 대해 읽고 쓸 파일을 가져옵니다.
         * 이 파일은 커밋되면 이 인덱스의 새 값이 됩니다.
         */
        fun file(index: Int): File {
            synchronized(this@DiskLruCache) {
                check(!closed) { "editor is closed" }
                return entry.dirtyFiles[index].toFile().apply { createNewFile() }
            }
        }

        /**
         * 이 편집기가 정상적으로 완료되지 않도록 합니다.
         * 이 편집기가 활성화되어 있는 동안 대상 항목이 축출되는 경우에 필요합니다.
         */
        fun detach() {
            if (entry.currentEditor == this) {
                entry.zombie = true //현재 수정이 완료될 때까지 삭제할 수 없습니다.
            }
        }

        /**
         * 독자가 볼 수 있도록 이 편집을 커밋합니다.
         * 이렇게 하면 편집 잠금이 해제되어 동일한 키에서 다른 편집이 시작될 수 있습니다.
         */
        fun commit() = complete(true)

        /**
         * 편집을 커밋하고 새 [Snapshot]을 원자적으로 엽니다.
         */
        fun commitAndGet(): Snapshot? {
            synchronized(this@DiskLruCache) {
                commit()
                return get(entry.key)
            }
        }

        /**
         * 이 편집을 중단합니다.
         * 이렇게 하면 편집 잠금이 해제되어 동일한 키에서 다른 편집이 시작될 수 있습니다.
         */
        fun abort() = complete(false)

        /**
         * 이 편집을 성공 또는 실패로 완료하십시오.
         */
        private fun complete(success: Boolean) {
            synchronized(this@DiskLruCache) {
                check(!closed) { "editor is closed" }
                if (entry.currentEditor == this) {
                    completeEdit(this, success)
                }
                closed = true
            }
        }
    }

    inner class Entry(val key: String) {

        /** 이 항목 파일의 길이입니다. */
        val lengths = LongArray(valueCount)
        val cleanFiles = ArrayList<Path>(valueCount)
        val dirtyFiles = ArrayList<Path>(valueCount)

        /** 이 항목이 게시된 적이 있으면 True입니다. */
        var readable = false

        /** 현재 편집 또는 읽기가 완료될 때 이 항목을 삭제해야 하는 경우 True입니다. */
        var zombie = false

        /**
         * 진행 중인 편집이거나 이 항목이 편집되지 않는 경우 null입니다. 이것을 null로 설정하면 항목이 좀비인 경우 제거해야 합니다.
         */
        var currentEditor: Editor? = null

        /**
         * 쓰기 또는 삭제를 계속하기 전에 현재 이 항목을 읽고 있는 스냅샷입니다.
         * 이것을 0으로 감소시킬 때 좀비인 경우 항목을 제거해야 합니다.
         */
        var lockingSnapshotCount = 0

        init {
            // 이름은 반복적이므로 할당을 피하기 위해 동일한 빌더를 다시 사용하십시오.
            val fileBuilder = StringBuilder(key).append('.')
            val truncateTo = fileBuilder.length
            for (i in 0 until valueCount) {
                fileBuilder.append(i)
                cleanFiles += directory / fileBuilder.toString()
                fileBuilder.append(".tmp")
                dirtyFiles += directory / fileBuilder.toString()
                fileBuilder.setLength(truncateTo)
            }
        }

        /** "10123"과 같은 십진수를 사용하여 길이를 설정합니다. */
        fun setLengths(strings: List<String>) {
            if (strings.size != valueCount) {
                throw IOException("unexpected journal line: $strings")
            }

            try {
                for (i in strings.indices) {
                    lengths[i] = strings[i].toLong()
                }
            } catch (_: NumberFormatException) {
                throw IOException("unexpected journal line: $strings")
            }
        }

        /** [writer]에 공백 접두어 길이를 추가합니다. */
        fun writeLengths(writer: BufferedSink) {
            for (length in lengths) {
                writer.writeByte(' '.code).writeDecimalLong(length)
            }
        }

        /** 이 항목의 스냅샷을 반환합니다. */
        fun snapshot(): Snapshot? {
            if (!readable) return null
            if (currentEditor != null || zombie) return null

            // 항목의 파일이 여전히 존재하는지 확인하십시오.
            cleanFiles.forEachIndices { file ->
                if (!fileSystem.exists(file)) {
                    // 항목이 더 이상 유효하지 않으므로 메타데이터가 정확하도록 제거하십시오.
                    // (즉, 캐시 크기)
                    try {
                        removeEntry(this)
                    } catch (_: IOException) {}
                    return null
                }
            }
            lockingSnapshotCount++
            return Snapshot(this)
        }
    }

    companion object {
        private const val JOURNAL_FILE = "journal"
        private const val JOURNAL_FILE_TEMP = "journal.tmp"
        private const val JOURNAL_FILE_BACKUP = "journal.bkp"
        private const val MAGIC = "libcore.io.DiskLruCache"
        private const val VERSION = "1"
        private const val CLEAN = "CLEAN"
        private const val DIRTY = "DIRTY"
        private const val REMOVE = "REMOVE"
        private const val READ = "READ"
        private val LEGAL_KEY_PATTERN = "[a-z0-9_-]{1,120}".toRegex()
    }
}
