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

package org.jraf.klibslack.internal.model

import org.jraf.klibslack.model.MessageAddedEvent
import org.jraf.klibslack.model.MessageChangedEvent
import org.jraf.klibslack.model.MessageDeletedEvent
import org.jraf.klibslack.model.ReactionAddedEvent
import org.jraf.klibslack.model.UnknownEvent

internal data class MessageAddedEventImpl(
  override val ts: String,
  override val user: String,
  override val channel: String,
  override val threadTs: String?,
  override val text: String,
) : MessageAddedEvent

internal data class MessageChangedEventImpl(
  override val ts: String,
  override val user: String,
  override val channel: String,
  override val threadTs: String?,
  override val previousText: String,
  override val newText: String,
  override val changedTs: String?,
) : MessageChangedEvent

internal data class MessageDeletedEventImpl(
  override val ts: String,
  override val user: String,
  override val channel: String,
  override val threadTs: String?,
  override val deletedText: String,
  override val deletedTs: String?,
) : MessageDeletedEvent

internal data class ReactionAddedEventImpl(
  override val user: String,
  override val reaction: String,
  override val eventTs: String,
  override val type: String,
  override val channel: String,
  override val ts: String,
) : ReactionAddedEvent

internal data class UnknownEventImpl(
  override val type: String,
) : UnknownEvent
