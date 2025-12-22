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

import org.jraf.klibnanolog.logi
import org.jraf.klibslack.client.SlackClient
import org.jraf.klibslack.client.configuration.ClientConfiguration
import org.jraf.klibslack.client.configuration.HttpConfiguration
import org.jraf.klibslack.client.configuration.HttpLoggingLevel

suspend fun main(av: Array<String>) {
  logi("Hello, world!")
  val (appToken, botUserOAuthToken) = av
  val slackClient = SlackClient.newInstance(
    ClientConfiguration(
      appToken = appToken,
      botUserOAuthToken = botUserOAuthToken,
      httpConfiguration = HttpConfiguration(
        loggingLevel = HttpLoggingLevel.ALL,
      ),
    ),
  )
  logi(slackClient.getAllMembers().toString())
  logi(slackClient.getAllChannels().toString())
  logi(slackClient.getBotIdentity().toString())

  val webSocketUrl = slackClient.appsConnectionsOpen()
  slackClient.openWebSocket(webSocketUrl) { event ->
    logi(event.toString())
    logi("")
  }
}
