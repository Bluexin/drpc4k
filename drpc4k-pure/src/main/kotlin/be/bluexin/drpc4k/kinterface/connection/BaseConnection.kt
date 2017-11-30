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

interface BaseConnection {
    /*
    static BaseConnection* Create();
    static void Destroy(BaseConnection*&);
    bool isOpen{false};
    bool Open();
    bool Close();
    bool Write(const void* data, size_t length);
    bool Read(void* data, size_t length);
     */

    val isOpen: Boolean
    fun open(): Boolean
    fun close(): Boolean
    fun write(bytes: ByteArray): Boolean
    fun read(toBytes: ByteArray, len: Int, wait: Boolean): Boolean

    companion object {
        fun create(): BaseConnection {
            TODO()
        }
    }
}