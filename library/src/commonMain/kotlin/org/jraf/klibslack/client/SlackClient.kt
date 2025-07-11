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

package org.jraf.klibslack.client

import org.jraf.klibslack.client.configuration.ClientConfiguration
import org.jraf.klibslack.internal.client.SlackClientImpl
import org.jraf.klibslack.model.Channel
import org.jraf.klibslack.model.Event
import org.jraf.klibslack.model.Identity
import org.jraf.klibslack.model.Member

interface SlackClient {
  companion object {
    @JvmStatic
    fun newInstance(configuration: ClientConfiguration): SlackClient =
      SlackClientImpl(configuration)
  }

  suspend fun getBotIdentity(): Identity

  suspend fun getUserIdentity(): Identity

  suspend fun getAllChannels(): List<Channel>

  suspend fun getAllMembers(): List<Member>

  suspend fun chatPostMessage(channel: String, text: String, threadTs: String? = null)

  suspend fun reactionsAdd(channel: String, timestamp: String, name: String)

  suspend fun appsConnectionsOpen(): String
  suspend fun openWebSocket(url: String, onEvent: suspend (event: Event) -> Unit)
}
