package org.zalando.react.nakadi

import org.zalando.react.nakadi.commit.OffsetTracking
import org.zalando.react.nakadi.commit.handlers.BaseCommitManager
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.Future


object InMemoryCommitCommitManager extends BaseCommitManager {

  private val store = scala.collection.concurrent.TrieMap.empty[String, String]
  private def generateKey(group: String, eventType: String, partition: String) = s"$group-$eventType-$partition"
  private def generateValue(offset: String, leaseHolder: String, leaseCounter: Long) = s"$offset-$leaseHolder-$leaseCounter"

  def create(key: String, partitionId: String, checkpointId: String, leaseHolder: String) = {
    val value = generateValue(checkpointId, leaseHolder, 0)
    store.put(key, value)
    OffsetTracking(
      partitionId = partitionId,
      checkpointId = value.split("-")(0),
      leaseHolder = value.split("-")(1),
      leaseCounter = Option(1),
      leaseTimestamp = new DateTime(DateTimeZone.UTC),
      leaseId = None
    )
  }

  def update(key: String, value: String, partitionId: String) = {
    val count = value.split("-")(2).toLong
    val leaseCounter = count + 1

    val offset = OffsetTracking(
      partitionId = partitionId,
      checkpointId = value.split("-")(0),
      leaseHolder = value.split("-")(1),
      leaseCounter = Option(leaseCounter),
      leaseTimestamp = new DateTime(DateTimeZone.UTC),
      leaseId = None
    )
    store.put(key, generateValue(offset.checkpointId, offset.leaseHolder, count))
    offset
  }

  override def put(groupId: String, eventType: String, offset: OffsetTracking): Future[OffsetTracking] = Future.successful {
    val key = generateKey(groupId, eventType, offset.partitionId)
    store.get(key)
      .fold(create(key, offset.partitionId, offset.checkpointId, offset.leaseHolder))(update(key, _, offset.partitionId))
  }

  override def get(groupId: String, eventType: String, partitionId: String): Future[Option[OffsetTracking]] = {
    Future.successful {
      val key = generateKey(groupId, eventType, partitionId)
      store.get(key).map { value =>
        OffsetTracking(
          partitionId = partitionId,
          checkpointId = value.split("-")(0),
          leaseHolder = value.split("-")(1),
          leaseCounter = Option(1),
          leaseTimestamp = new DateTime(DateTimeZone.UTC),
          leaseId = None
        )
      }
    }
  }
}