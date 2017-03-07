package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future


case class Similarity (
                          itemId: String,
                          anotherItemId: String,
                          similarity: Double
                          )

class SimilaritiesTable extends CassandraTable[Similarities, Similarity] {

  object itemId extends StringColumn(this) with PartitionKey[String]
  object similarity extends DoubleColumn(this) with ClusteringOrder[Double] with Descending
  object anotherItemId extends StringColumn(this) with ClusteringOrder[String] with Ascending


  def fromRow(row: Row): Similarity = {
    Similarity(
      itemId(row),
      anotherItemId(row),
      similarity(row)
    )
  }
}

abstract class Similarities extends SimilaritiesTable with RootConnector {

  def store(similarity: Similarity): Future[ResultSet] = {
    insert.value(_.itemId, similarity.itemId)
          .value(_.anotherItemId, similarity.anotherItemId)
          .value(_.similarity, similarity.similarity)
          .consistencyLevel_=(ConsistencyLevel.ALL)
          .future()
  }

  def deleteRow(similarity: Similarity): Future[ResultSet] = {
    delete.where(_.itemId eqs similarity.itemId)
            .and(_.similarity eqs similarity.similarity)
            .and(_.anotherItemId eqs similarity.anotherItemId)
            .future()
  }

  def getById(id: String, limit: Int = 10): Future[Seq[Similarity]] = {
    select.where(_.itemId eqs id).limit(limit).fetch()
  }
}