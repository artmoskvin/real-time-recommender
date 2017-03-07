package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future


case class TrendingItemCount (
                             category: String,
                             itemId: String,
                             count: Long
                             )

class TrendingItemCountsTable extends CassandraTable[TrendingItemCounts, TrendingItemCount] {

  object category extends StringColumn(this) with PartitionKey[String]
  object count extends LongColumn(this) with ClusteringOrder[Long] with Descending
  object itemId extends StringColumn(this) with ClusteringOrder[String] with Ascending

  def fromRow(row: Row): TrendingItemCount = {
    TrendingItemCount(
      category(row),
      itemId(row),
      count(row)
    )
  }
}

abstract class TrendingItemCounts extends TrendingItemCountsTable with RootConnector {

  def store(itemCount: TrendingItemCount): Future[ResultSet] = {
    insert.value(_.itemId, itemCount.itemId).value(_.count, itemCount.count).value(_.category, itemCount.category)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getById(id: String): Future[Option[TrendingItemCount]] = {
    select.where(_.itemId eqs id).one()
  }

//  def incrementCount(itemId: String, deltaWeight: Int): Future[ResultSet] = {
//    update.where(_.itemId eqs itemId).modify(_.count += deltaWeight).future()
//  }

  def getAll(limit: Int = 100): Future[Seq[TrendingItemCount]] = {
    select.limit(limit).fetch()
  }

  def deleteRow(itemCount: TrendingItemCount): Future[ResultSet] = {
    delete.where(_.itemId eqs itemCount.itemId)
          .and(_.count eqs itemCount.count)
          .and(_.category eqs itemCount.category)
          .future()
  }

}