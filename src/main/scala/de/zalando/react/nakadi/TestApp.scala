package de.zalando.react.nakadi

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import de.zalando.react.nakadi.NakadiMessages.{ConsumerMessage, Offset}
import de.zalando.react.nakadi.commit.handlers.aws.DynamoDBHandler

import scala.concurrent.duration._


object TestApp extends App {

  val token = ""

  val config = ConfigFactory.load()

  implicit val system = ActorSystem("reactive-nakadi")
  implicit val materializer = ActorMaterializer()

  val counterList = scala.collection.mutable.ListBuffer[Int]()

  val nakadi = new ReactiveNakadi()

  val publisher: PublisherWithCommitSink = nakadi.consumeWithOffsetSink(ConsumerProperties(
    server = "nakadi-sandbox.aruha-test.zalan.do",
    securedConnection = true,
    tokenProvider = () => token,
    topic = "reactive-nakadi-testing",
    groupId = "some-group",
    partition = "0",
    commitHandler = new DynamoDBHandler(system),
    offset = Some(Offset("141")),
    sslVerify = false,
    port = 443,
    urlSchema = "https://",
    commitInterval = Some(10.seconds)
  ))

  def throttle(msg: ConsumerMessage) = {
    Thread.sleep(1000)
    msg
  }

  def echo(msg: ConsumerMessage) = {
    println(s"From consumer: $msg")
    counterList += 0
    msg
  }

  def counterPrint(msg: ConsumerMessage) = {
    println(s"count: ${counterList.length}")
    msg
  }

  Source
    .fromPublisher(publisher.publisher)
    .map(throttle)
    .map(echo)
    .to(Sink.ignore)
    .run()

}
