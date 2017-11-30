/*
 * Copyright (c) 2017 Arnaud 'Bluexin' Solé
 *
 * This file is part of drpc4k.
 *
 * drpc4k is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * drpc4k is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drpc4k.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.bluexin.drpc4k.kinterface.connection

import java.io.IOException
import java.io.RandomAccessFile

class WinConnection: BaseConnection {

    private var pipe: RandomAccessFile? = null

    override val isOpen: Boolean
        get() = pipe != null

    override fun open(): Boolean {
        if (isOpen) throw IllegalStateException("Connection is already opened.")

        var pipeDigit = 0
        while (pipeDigit < 10) {
            try {
                this.pipe = RandomAccessFile(pipeName + pipeDigit, "rw")
                return true
            } catch (ex: Exception) {
                ++pipeDigit
            }
        }

        return false
    }

    override fun close(): Boolean {
        if (isOpen) return true

        try {
            pipe!!.close()
        } catch (_: IOException) {}

        pipe = null

        return true
    }

    override fun write(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        if (!isOpen) return false

        return try {
            this.pipe!!.write(bytes, 0, bytes.size)
            true
        } catch (_: IOException) {
            false
        }
    }

    override fun read(toBytes: ByteArray, len: Int, wait: Boolean): Boolean {
        if (toBytes.isEmpty()) return true
        if (!this.isOpen) return false

        try {
            if (!wait) {
                val available = this.pipe!!.length() - this.pipe!!.filePointer

                if (available < len) return false
            }

            val read = this.pipe!!.read(toBytes, 0, len)

            if (read != len)
                throw IOException()

            return true
        } catch (ignored: IOException) {
            this.close()
            return false
        }
    }

    private companion object {
        const val pipeName = "\\\\?\\pipe\\discord-ipc-"
    }
}