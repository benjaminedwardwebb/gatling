/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.engine

import io.gatling.commons.util.SystemProps._
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.client.{ HttpClient, HttpClientConfig }
import io.gatling.http.client.impl.DefaultHttpClient
import io.gatling.http.util._

import com.typesafe.scalalogging.StrictLogging

private[gatling] object HttpClientFactory {

  def apply(coreComponents: CoreComponents, sslContextsFactory: SslContextsFactory): HttpClientFactory =
    coreComponents.configuration.resolve(
      // [fl]
      //
      //
      //
      //
      // [fl]
      new DefaultHttpClientFactory(sslContextsFactory, coreComponents.configuration)
    )
}

private[gatling] trait HttpClientFactory {

  def newClient: HttpClient
}

private[gatling] class DefaultHttpClientFactory(sslContextsFactory: SslContextsFactory, configuration: GatlingConfiguration)
    extends HttpClientFactory
    with StrictLogging {

  private val httpConfig = configuration.http
  setSystemPropertyIfUndefined("io.netty.allocator.type", httpConfig.advanced.allocator)
  setSystemPropertyIfUndefined("io.netty.maxThreadLocalCharBufferSize", httpConfig.advanced.maxThreadLocalCharBufferSize)

  private[gatling] def newClientConfig(): HttpClientConfig = {

    val SslContexts(defaultSslContext, defaultAlpnSslContext) = sslContextsFactory.newSslContexts(http2Enabled = true, None)
    new HttpClientConfig()
      .setDefaultSslContext(defaultSslContext)
      .setDefaultAlpnSslContext(defaultAlpnSslContext.orNull)
      .setConnectTimeout(httpConfig.advanced.connectTimeout.toMillis)
      .setHandshakeTimeout(httpConfig.advanced.handshakeTimeout.toMillis)
      .setChannelPoolIdleTimeout(httpConfig.advanced.pooledConnectionIdleTimeout.toMillis)
      .setEnableSni(httpConfig.advanced.enableSni)
      .setEnableHostnameVerification(httpConfig.advanced.enableHostnameVerification)
      .setDefaultCharset(configuration.core.charset)
      .setUseNativeTransport(httpConfig.advanced.useNativeTransport)
      .setTcpNoDelay(httpConfig.advanced.tcpNoDelay)
      .setSoKeepAlive(httpConfig.advanced.soKeepAlive)
      .setSoReuseAddress(httpConfig.advanced.soReuseAddress)
      .setThreadPoolName("gatling-http")
  }

  override def newClient: HttpClient = new DefaultHttpClient(newClientConfig())
}
