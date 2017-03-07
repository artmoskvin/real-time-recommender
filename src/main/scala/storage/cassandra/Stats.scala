package storage.cassandra

import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._


case class Stat(
               event: String,
               userId: String,
               itemId: String,
               timestamp: DateTime,
               recommendationId: String,
               price: Double
               )

class StatsTable extends CassandraTable[Stats, Stat] {

  object event extends StringColumn(this) with PartitionKey[String]
  object userId extends StringColumn(this)
  object itemId extends StringColumn(this)
  object timestamp extends DateTimeColumn(this) with ClusteringOrder[DateTime] with Descending
  object recommendationId extends StringColumn(this)
  object price extends DoubleColumn(this)

  def fromRow(row: Row): Stat = {
    Stat(
      event(row),
      userId(row),
      itemId(row),
      timestamp(row),
      recommendationId(row),
      price(row)
    )
  }

}

abstract class Stats extends StatsTable with RootConnector {

  def store(stat: Stat): Future[ResultSet] = {
    insert.value(_.event, stat.event)
          .value(_.userId, stat.userId)
          .value(_.timestamp, stat.timestamp)
          .value(_.recommendationId, stat.recommendationId)
          .value(_.itemId, stat.itemId)
          .value(_.price, stat.price)
          .consistencyLevel_=(ConsistencyLevel.ALL)
          .future()
  }

  def getEvents(event: String, from: DateTime, to: DateTime): Future[Seq[Stat]] = {
    select
      .where(_.event eqs event)
      .and(_.timestamp gte from)
      .and(_.timestamp lte to)
      .fetch()
  }

  def getEventsFromRecommendation(event: String, from: DateTime, to: DateTime): Future[Seq[Stat]] = {
    select
      .where(_.event eqs event)
      .and(_.timestamp gte from)
      .and(_.timestamp lte to)
      .and(_.recommendationId isGt "")
      .allowFiltering()
      .fetch()
  }

  def getSales(from: DateTime, to: DateTime): Future[Seq[Stat]] = {
    select
      .where(_.event eqs "buy")
      .and(_.timestamp gte from)
      .and(_.timestamp lte to)
      .fetch()
  }

  def getSalesFromRecommendation(from: DateTime, to: DateTime): Future[Seq[Stat]] = {
    select
      .where(_.event eqs "buy")
      .and(_.timestamp gte from)
      .and(_.timestamp lte to)
      .and(_.recommendationId isGt "")
      .allowFiltering()
      .fetch()
  }

}
