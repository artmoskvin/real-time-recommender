package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future

case class BestSellerIndex(
                          itemId: String,
                          score: Long
                          )

class BestSellersIndexTable extends CassandraTable[BestSellersIndex, BestSellerIndex] {

  object itemId extends StringColumn(this) with PartitionKey[String]
  object score extends CounterColumn(this)

  def fromRow(row: Row): BestSellerIndex = {
    BestSellerIndex(
      itemId(row),
      score(row)
    )
  }

}

abstract class BestSellersIndex extends BestSellersIndexTable with RootConnector {

  def store(bestSellerIndex: BestSellerIndex): Future[ResultSet] = {
    insert.value(_.itemId, bestSellerIndex.itemId).value(_.score, bestSellerIndex.score)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def incrementCount(itemId: String, delta: Int = 1): Future[ResultSet] = {
    update.where(_.itemId eqs itemId).modify(_.score += delta).future()
  }

  def getById(itemId: String): Future[Option[BestSellerIndex]] = {
    select.where(_.itemId eqs itemId).one()
  }

}
