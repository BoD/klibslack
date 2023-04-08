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

package org.jraf.klibslack.internal.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jraf.klibslack.client.SlackClient
import org.jraf.klibslack.client.configuration.ClientConfiguration
import org.jraf.klibslack.client.configuration.HttpLoggingLevel
import org.jraf.klibslack.internal.json.JsonChatPostMessageRequest
import org.jraf.klibslack.internal.json.JsonEvent
import org.jraf.klibslack.internal.json.JsonMember
import org.jraf.klibslack.internal.json.JsonReactionAddRequest
import org.jraf.klibslack.internal.model.MemberImpl
import org.jraf.klibslack.model.Event
import org.jraf.klibslack.model.Member
import org.slf4j.LoggerFactory

internal class SlackClientImpl(private val clientConfiguration: ClientConfiguration) : SlackClient {
  private val LOGGER = LoggerFactory.getLogger(SlackClientImpl::class.java)

  private val service: SlackService by lazy {
    SlackService(
      provideHttpClient(clientConfiguration)
    )
  }

  private fun provideHttpClient(clientConfiguration: ClientConfiguration): HttpClient {
    val json = Json {
      ignoreUnknownKeys = true
      useAlternativeNames = false
      serializersModule = SerializersModule {
        polymorphic(JsonEvent::class) {
          defaultDeserializer { JsonEvent.JsonUnknownEvent.serializer() }
        }
      }
    }
    return HttpClient {
      install(ContentNegotiation) {
        json(json)
      }
      install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 60_000
        socketTimeoutMillis = 60_000
      }
      install(WebSockets) {
        pingInterval = 60_000
        contentConverter = KotlinxWebsocketSerializationConverter(json)
      }
      engine {
        // Setup a proxy if requested
        clientConfiguration.httpConfiguration.httpProxy?.let { httpProxy ->
          proxy = ProxyBuilder.http(URLBuilder().apply {
            host = httpProxy.host
            port = httpProxy.port
          }.build())
        }
      }
      // Setup logging if requested
      if (clientConfiguration.httpConfiguration.loggingLevel != HttpLoggingLevel.NONE) {
        install(Logging) {
          logger = object : Logger {
            private val delegate = LoggerFactory.getLogger(SlackClient::class.java)!!
            override fun log(message: String) {
              delegate.debug(message)
            }
          }
          level = when (clientConfiguration.httpConfiguration.loggingLevel) {
            HttpLoggingLevel.NONE -> LogLevel.NONE
            HttpLoggingLevel.INFO -> LogLevel.INFO
            HttpLoggingLevel.HEADERS -> LogLevel.HEADERS
            HttpLoggingLevel.BODY -> LogLevel.BODY
            HttpLoggingLevel.ALL -> LogLevel.ALL
          }
        }
      }
    }
  }

  override suspend fun getAllMembers(): List<Member> {
    return try {
      val memberList = mutableListOf<JsonMember>()
      var cursor: String? = null
      do {
        LOGGER.debug("Calling usersList cursor=$cursor")
        val response = service.usersList(botUserOAuthToken = clientConfiguration.botUserOAuthToken, cursor = cursor)
        memberList += response.members
        cursor = response.response_metadata?.next_cursor?.ifBlank { null }
        // Avoid hitting the rate limit
        delay(3000)
      } while (cursor != null)
      memberList.map {
        MemberImpl(
          id = it.id,
          name = it.name,
          realName = it.profile.real_name,
          displayName = it.profile.display_name,
          isBot = it.is_bot
        )
      }
    } catch (e: Exception) {
      LOGGER.warn("Could not make network call", e)
      throw e
    }
  }

  override suspend fun appsConnectionsOpen(): String {
    return service.appsConnectionsOpen(clientConfiguration.appToken).url
  }

  override suspend fun openWebSocket(url: String, onEvent: suspend (event: Event) -> Unit) {
    service.openWebSocket(url, onEvent)
  }

  override suspend fun chatPostMessage(channel: String, text: String, threadTs: String?) {
    service.chatPostMessage(
      clientConfiguration.botUserOAuthToken,
      JsonChatPostMessageRequest(channel = channel, text = text, thread_ts = threadTs)
    )
  }

  override suspend fun reactionsAdd(channel: String, timestamp: String, name: String) {
    service.reactionsAdd(
      clientConfiguration.botUserOAuthToken,
      JsonReactionAddRequest(channel = channel, timestamp = timestamp, name = name)
    )
  }
}
