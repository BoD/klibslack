/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2023-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 * and contributors (https://github.com/BoD/klibnotion/graphs/contributors)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jraf.klibslack.internal.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
internal sealed interface JsonEvent {
  @Serializable
  @SerialName("message")
  data class JsonMessageEvent(
    val user: String? = null,
    val channel: String,
    val text: String? = null,
    val ts: String,
    val thread_ts: String? = null,
    val subtype: String? = null,
    val previous_message: JsonMessageEditPreviousMessage? = null,
    val message: JsonMessageEditNewMessage? = null,
  ) : JsonEvent {
    @Serializable
    data class JsonMessageEditPreviousMessage(
      val ts: String,
      val user: String,
      val text: String,
      val thread_ts: String? = null,
    )

    @Serializable
    data class JsonMessageEditNewMessage(
      val text: String,
    )
  }

  @Serializable
  @SerialName("reaction_added")
  data class JsonReactionAddedEvent(
    val user: String,
    val reaction: String = "",
    val item: JsonReactionItem,
    val event_ts: String,
  ) : JsonEvent {
    @Serializable
    data class JsonReactionItem(
      val type: String,
      val channel: String = "",
      val ts: String = "",
    )
  }

  @Serializable
  data class JsonUnknownEvent(
    val type: String,
  ) : JsonEvent
}
