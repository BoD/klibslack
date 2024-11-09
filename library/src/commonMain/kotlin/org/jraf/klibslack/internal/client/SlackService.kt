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
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.converter
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.WebsocketContentConverter
import io.ktor.serialization.WebsocketConverterNotFoundException
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.serialization.suitableCharset
import io.ktor.util.reflect.typeInfo
import io.ktor.utils.io.charsets.Charset
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import org.jraf.klibslack.internal.json.JsonAcknowledge
import org.jraf.klibslack.internal.json.JsonAppsConnectionsOpenResponse
import org.jraf.klibslack.internal.json.JsonChatPostMessageRequest
import org.jraf.klibslack.internal.json.JsonConversationsListResponse
import org.jraf.klibslack.internal.json.JsonEvent
import org.jraf.klibslack.internal.json.JsonPayloadEnvelope
import org.jraf.klibslack.internal.json.JsonReactionAddRequest
import org.jraf.klibslack.internal.json.JsonUsersListResponse
import org.jraf.klibslack.internal.model.MessageAddedEventImpl
import org.jraf.klibslack.internal.model.MessageChangedEventImpl
import org.jraf.klibslack.internal.model.MessageDeletedEventImpl
import org.jraf.klibslack.internal.model.ReactionAddedEventImpl
import org.jraf.klibslack.internal.model.UnknownEventImpl
import org.jraf.klibslack.model.Event
import org.slf4j.LoggerFactory

internal class SlackService(
  private val httpClient: HttpClient,
) {
  companion object {
    private const val URL_BASE = "https://slack.com/api"
  }

  private val LOGGER = LoggerFactory.getLogger(SlackClientImpl::class.java)

  // https://api.slack.com/methods/apps.connections.open
  suspend fun appsConnectionsOpen(appToken: String): JsonAppsConnectionsOpenResponse {
    return httpClient.post("$URL_BASE/apps.connections.open") {
      header("Authorization", "Bearer $appToken")
      contentType(ContentType.Application.Json)
    }.body()
  }

  // https://api.slack.com/methods/users.list
  suspend fun usersList(botUserOAuthToken: String, cursor: String? = null, limit: Int = 1000): JsonUsersListResponse {
    return httpClient.get("$URL_BASE/users.list") {
      header("Authorization", "Bearer $botUserOAuthToken")
      parameter("cursor", cursor)
      parameter("limit", limit)
      contentType(ContentType.Application.Json)
    }.body()
  }

  // https://api.slack.com/methods/conversations.list
  suspend fun conversationsList(botUserOAuthToken: String, cursor: String? = null, limit: Int = 1000): JsonConversationsListResponse {
    return httpClient.get("$URL_BASE/conversations.list") {
      header("Authorization", "Bearer $botUserOAuthToken")
      parameter("types", "public_channel,private_channel")
      parameter("cursor", cursor)
      parameter("limit", limit)
      contentType(ContentType.Application.Json)
    }.body()
  }

  // https://api.slack.com/methods/chat.postMessage
  suspend fun chatPostMessage(botUserOAuthToken: String, request: JsonChatPostMessageRequest) {
    httpClient.post("$URL_BASE/chat.postMessage") {
      header("Authorization", "Bearer $botUserOAuthToken")
      contentType(ContentType.Application.Json)
      setBody(request)
    }
  }

  // https://api.slack.com/methods/reactions.add
  suspend fun reactionsAdd(botUserOAuthToken: String, request: JsonReactionAddRequest) {
    httpClient.post("$URL_BASE/reactions.add") {
      header("Authorization", "Bearer $botUserOAuthToken")
      contentType(ContentType.Application.Json)
      setBody(request)
    }
  }

  suspend fun openWebSocket(url: String, onEvent: suspend (event: Event) -> Unit) {
    httpClient.webSocket(url) {
      // Ignore hello message
      val helloMessage = incoming.receive() as Frame.Text
      LOGGER.debug("WebSocket in: ${helloMessage.readText()}")
      while (true) {
        val payloadEnvelope = try {
          myReceiveDeserialized<JsonPayloadEnvelope>()
        } catch (e: Exception) {
          LOGGER.error("Error while receiving message", e)
          break
        }
        LOGGER.debug("WebSocket in: $payloadEnvelope")

        if (payloadEnvelope.type == "disconnect") {
          LOGGER.warn("Disconnect message received, closing WebSocket")
          throw Exception("Disconnect message received")
        }

        payloadEnvelope.envelope_id?.let { envelopeId ->
          val ack = JsonAcknowledge(envelope_id = envelopeId)
          LOGGER.debug("WebSocket out: $ack")
          sendSerialized(ack)
        }
        onEvent(
          payloadEnvelope.payload!!.event.let { jsonEvent ->
            when (jsonEvent) {
              is JsonEvent.JsonMessageEvent ->
                when (jsonEvent.subtype) {
                  null -> MessageAddedEventImpl(
                    user = jsonEvent.user!!,
                    channel = jsonEvent.channel,
                    text = jsonEvent.text!!,
                    ts = jsonEvent.ts,
                    threadTs = jsonEvent.thread_ts,
                  )

                  "message_changed" -> MessageChangedEventImpl(
                    ts = jsonEvent.ts,
                    user = jsonEvent.previous_message!!.user,
                    channel = jsonEvent.channel,
                    threadTs = jsonEvent.previous_message.thread_ts,
                    previousText = jsonEvent.previous_message.text,
                    newText = jsonEvent.message!!.text,
                    changedTs = jsonEvent.previous_message.ts,
                  )

                  "message_deleted" -> MessageDeletedEventImpl(
                    ts = jsonEvent.ts,
                    user = jsonEvent.previous_message!!.user,
                    channel = jsonEvent.channel,
                    threadTs = jsonEvent.previous_message.thread_ts,
                    deletedText = jsonEvent.previous_message.text,
                    deletedTs = jsonEvent.previous_message.ts,
                  )

                  else -> {
                    LOGGER.debug("Ignoring message event of subtype ${jsonEvent.subtype}")
                    UnknownEventImpl(
                      type = "message/${jsonEvent.subtype}",
                    )
                  }
                }

              is JsonEvent.JsonReactionAddedEvent -> ReactionAddedEventImpl(
                user = jsonEvent.user,
                reaction = jsonEvent.reaction,
                eventTs = jsonEvent.event_ts,
                type = jsonEvent.item.type,
                channel = jsonEvent.item.channel,
                ts = jsonEvent.item.ts,
              )

              is JsonEvent.JsonUnknownEvent -> UnknownEventImpl(
                type = jsonEvent.type,
              )
            }
          }
        )
      }
    }
  }

  private suspend inline fun <reified T> DefaultClientWebSocketSession.myReceiveDeserialized(): T {
    val converter = converter
      ?: throw WebsocketConverterNotFoundException("No converter was found for websocket")

    return myReceiveDeserializedBase<T>(
      converter,
      call.request.headers.suitableCharset()
    ) as T
  }

  private suspend inline fun <reified T> WebSocketSession.myReceiveDeserializedBase(
    converter: WebsocketContentConverter,
    charset: Charset,
  ): Any? {
    val frame = incoming.receive()
    LOGGER.debug("\n\n\n\n\n*************************\nReceived frame: ${(frame as Frame.Text).readText()}")

    if (!converter.isApplicable(frame)) {
      throw WebsocketDeserializeException(
        "Converter doesn't support frame type ${frame.frameType.name}",
        frame = frame
      )
    }

    val typeInfo = typeInfo<T>()
    val result = converter.deserialize(
      charset = charset,
      typeInfo = typeInfo,
      content = frame
    )

    if (result is T) return result
    if (result == null) {
      if (typeInfo.kotlinType?.isMarkedNullable == true) return null
      throw WebsocketDeserializeException("Frame has null content", frame = frame)
    }

    throw WebsocketDeserializeException(
      "Can't deserialize value : expected value of type ${T::class.simpleName}," +
        " got ${result::class.simpleName}",
      frame = frame
    )
  }
}
