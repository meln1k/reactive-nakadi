package org.zalando.react.nakadi.client.providers

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}

import akka.actor.ActorContext
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.{Http, HttpsConnectionContext}
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scala.concurrent.duration._


class HttpClientProvider(actorContext: ActorContext,
                         server: String, port: Int,
                         isConnectionSSL: Boolean = false,
                         acceptAnyCertificate: Boolean = false,
                         connectionTimeout: FiniteDuration) {

  val http = Http(actorContext.system)

  val connectionSettings = {
    ClientConnectionSettings
      .apply(actorContext.system)
      .withConnectingTimeout(connectionTimeout)
      .withIdleTimeout(Duration.Inf)
  }

  val connectionContext = {
    isConnectionSSL match {
      case true =>
        val sslContext = if (acceptAnyCertificate) SSLContext.getDefault else {

          val permissiveTrustManager: TrustManager = new X509TrustManager() {
            override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
            override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
            override def getAcceptedIssuers(): Array[X509Certificate] = Array.empty
          }

          val ctx = SSLContext.getInstance("TLS")
          ctx.init(Array.empty, Array(permissiveTrustManager), new SecureRandom())
          ctx
        }
        Some(new HttpsConnectionContext(sslContext))
      case false =>
        None
    }
  }


  val connection: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] = {

    connectionContext map { context =>
      http.outgoingConnectionHttps(server, port, context, settings = connectionSettings)
    } getOrElse {
      http.outgoingConnection(server, port, settings = connectionSettings)
    }
  }

}
