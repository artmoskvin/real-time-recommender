package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future


case class ItemCount (
                     itemId: String,
                     count: Long
                     )

class ItemCountsTable extends CassandraTable[ItemCounts, ItemCount] {

  object itemId extends StringColumn(this) with PartitionKey[String]
  object count extends CounterColumn(this)

  def fromRow(row: Row): ItemCount = {
    ItemCount(
      itemId(row),
      count(row)
    )
  }
}

abstract class ItemCounts extends ItemCountsTable with RootConnector {

  def store(itemCount: ItemCount): Future[ResultSet] = {
    insert.value(_.itemId, itemCount.itemId).value(_.count, itemCount.count)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getById(id: String): Future[Option[ItemCount]] = {
    select.where(_.itemId eqs id).one()
  }

  def incrementCount(itemId: String, deltaWeight: Int): Future[ResultSet] = {
    update.where(_.itemId eqs itemId).modify(_.count += deltaWeight).future()
  }

}

//abstract class TrendingItemCounts extends ItemCountsTable with RootConnector {
//
//  def store(itemCount: ItemCount): Future[ResultSet] = {
//    insert.value(_.itemId, itemCount.itemId).value(_.count, itemCount.count)
//      .consistencyLevel_=(ConsistencyLevel.ALL)
//      .future()
//  }
//
//  def getById(id: String): Future[Option[ItemCount]] = {
//    select.where(_.itemId eqs id).one()
//  }
//
//  def incrementCount(itemId: String, deltaWeight: Int): Future[ResultSet] = {
//    update.where(_.itemId eqs itemId).modify(_.count += deltaWeight).future()
//  }
//
//  def getAll(limit: Int = 100): Future[Seq[ItemCount]] = {
//    select.limit(limit).fetch()
//  }
//
//  def deleteById(itemId: String): Future[ResultSet] = {
//    delete.where(_.itemId eqs itemId).future()
//  }
//
//}
