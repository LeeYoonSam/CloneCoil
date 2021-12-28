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
@file:JvmName("-FileSystems")

package com.ys.coil.util

import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path

/** 예상하지만 존재하지 않아도 되는 파일을 삭제합니다. */
@OptIn(ExperimentalFileSystem::class)
internal fun FileSystem.deleteIfExists(path: Path) {
    try {
        delete(path)
    } catch (_: FileNotFoundException) {}
}

/** 허용 삭제, 실패 후에도 가능한 한 많은 파일을 지우십시오. */
@OptIn(ExperimentalFileSystem::class)
internal fun FileSystem.deleteContents(directory: Path) {
    var exception: IOException? = null
    val files = try {
        list(directory)
    } catch (_: FileNotFoundException) {
        return
    }
    for (file in files) {
        try {
            if (metadata(file).isDirectory) {
                deleteContents(file)
            }

            delete(file)
        } catch (e: IOException) {
            if (exception == null) {
                exception = e
            }
        }
    }
    if (exception != null) {
        throw exception
    }
}
