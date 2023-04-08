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

package org.jraf.klibslack.model

sealed interface Event

interface MessageAddedEvent : Event {
  val ts: String
  val user: String
  val channel: String
  val threadTs: String?
  val text: String
}

interface MessageChangedEvent : Event {
  val ts: String
  val user: String
  val channel: String
  val threadTs: String?
  val previousText: String
  val newText: String
  val changedTs: String?
}

interface MessageDeletedEvent : Event {
  val ts: String
  val user: String
  val channel: String
  val threadTs: String?
  val deletedText: String
  val deletedTs: String?
}

interface ReactionAddedEvent : Event {
  val user: String
  val reaction: String
  val eventTs: String
  val type: String
  val channel: String
  val ts: String
}

interface UnknownEvent : Event {
  val type: String
}
