package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future

case class BestSeller(
                     category: String,
                     itemId: String,
                     count: Long
                     )

class BestSellersTable extends CassandraTable[BestSellers, BestSeller] {

  object category extends StringColumn(this) with PartitionKey[String]
  object count extends LongColumn(this) with ClusteringOrder[Long] with Descending
  object itemId extends StringColumn(this) with ClusteringOrder[String] with Ascending

  def fromRow(row: Row): BestSeller = {
    BestSeller(
      category(row),
      itemId(row),
      count(row)
    )
  }

}

abstract class BestSellers extends BestSellersTable with RootConnector {

  def store(bestSeller: BestSeller): Future[ResultSet] = {
    insert.value(_.itemId, bestSeller.itemId).value(_.count, bestSeller.count).value(_.category, bestSeller.category)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getMany(limit: Int = 100): Future[Seq[BestSeller]] = {
    select.limit(limit).fetch()
  }

  def deleteRow(bestSeller: BestSeller): Future[ResultSet] = {
    delete.where(_.category eqs bestSeller.category).and(_.itemId eqs bestSeller.itemId).and(_.count eqs bestSeller.count).future()
  }

}
