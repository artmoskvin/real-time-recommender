package storage.cassandra

import scala.math.Ordering
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future


case class PairCount (
                       pairId: String,
                       count: Long
                     )

class PairCountsTable extends CassandraTable[PairCounts, PairCount] {

  object pairId extends StringColumn(this) with PartitionKey[String]
  object count extends CounterColumn(this)

  def fromRow(row: Row): PairCount = {
    PairCount(
      pairId(row),
      count(row)
    )
  }
}

abstract class PairCounts extends PairCountsTable with RootConnector {

  def store(pairCount: PairCount): Future[ResultSet] = {
    insert.value(_.pairId, pairCount.pairId).value(_.count, pairCount.count)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getById(id: String): Future[Option[PairCount]] = {
    select.where(_.pairId eqs id).one()
  }

  def incrementCount(pairId: String, deltaWeight: Int): Future[ResultSet] = {
    update.where(_.pairId eqs pairId).modify(_.count += deltaWeight).future()
  }

}
