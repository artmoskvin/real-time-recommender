package storage.cassandra

import scala.concurrent.Await
import scala.concurrent.duration._
import com.websudos.phantom.dsl._
import config.Config


object DefaultConnector {

  val hosts = Seq(Config.CASSANDRA_HOST)

  val connector = ContactPoints(hosts).keySpace(Config.CASSANDRA_KEYSPACE)

}

class CassandraStorage(val keyspace: KeySpaceDef) extends Database(keyspace) {
  object users extends Users with keyspace.Connector
  object itemCounts extends ItemCounts with keyspace.Connector
  object pairCounts extends PairCounts with keyspace.Connector
  object similarities extends Similarities with keyspace.Connector
  object similaritiesIndex extends SimilaritiesIndex with keyspace.Connector
  object trendingItemCounts extends TrendingItemCounts with keyspace.Connector
  object userViews extends Views with keyspace.Connector
  object bestSellers extends BestSellers with keyspace.Connector
  object bestSellersIndex extends BestSellersIndex with keyspace.Connector
  object stats extends Stats with keyspace.Connector
}

object CassandraStorage extends CassandraStorage(DefaultConnector.connector) {
  Await.result(CassandraStorage.autocreate.future(), 5.seconds)
}


