/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.softartdev.poder.media

import android.os.Parcel
import android.os.Parcelable
import android.support.v4.media.MediaMetadataCompat

/**
 * Holder class that encapsulates a MediaMetadataCompat and allows the actual metadata to be modified
 * without requiring to rebuild the collections the metadata is in.
 */
class Podcast(private val podcastId: Long, var metadata: MediaMetadataCompat, var sortKey: Long?) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != Podcast::class.java) {
            return false
        }
        val that = other as? Podcast
        return podcastId == that?.podcastId
    }

    override fun hashCode(): Int = podcastId.hashCode()

    override fun describeContents(): Int = 0

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(podcastId)
        out.writeParcelable(metadata, flags)
        out.writeLong(sortKey ?: 0L)
    }

    companion object CREATOR : Parcelable.Creator<Podcast> {
        override fun createFromParcel(parcel: Parcel) = Podcast(
                podcastId = parcel.readLong(),
                metadata = parcel.readParcelable(MediaMetadataCompat::class.java.classLoader),
                sortKey = parcel.readLong())
        override fun newArray(size: Int) = arrayOfNulls<Podcast>(size)
    }
}
