package de.zalando.react.nakadi.commit.handlers.aws

import com.amazonaws.regions.Regions
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import de.zalando.react.nakadi.properties.CommitProperties


trait Provider {
  def client: DynamoDB
  def leaseProperties: CommitProperties
}

class ClientProvider(override val leaseProperties: CommitProperties) extends Provider {

  override val client: DynamoDB = {
    val c = new AmazonDynamoDBClient(new ProfileCredentialsProvider())
    c.withRegion(Regions.fromName(leaseProperties.awsCommitRegion))
    new DynamoDB(c)
  }
}