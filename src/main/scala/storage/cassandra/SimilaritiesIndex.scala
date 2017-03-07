package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future


case class SimilarityIndex (
                        pairId: String,
                        similarity: Double
                      )

class SimilaritiesIndexTable extends CassandraTable[SimilaritiesIndex, SimilarityIndex] {

  object pairId extends StringColumn(this) with PartitionKey[String]
  object similarity extends DoubleColumn(this)


  def fromRow(row: Row): SimilarityIndex = {
    SimilarityIndex(
      pairId(row),
      similarity(row)
    )
  }
}

abstract class SimilaritiesIndex extends SimilaritiesIndexTable with RootConnector {

  def store(similarityIndex: SimilarityIndex): Future[ResultSet] = {
    insert.value(_.pairId, similarityIndex.pairId)
      .value(_.similarity, similarityIndex.similarity)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getById(pairId: String): Future[Option[SimilarityIndex]] = {
    select.where(_.pairId eqs pairId).one()
  }
}