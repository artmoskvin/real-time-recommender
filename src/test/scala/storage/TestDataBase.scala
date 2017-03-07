package storage

import com.websudos.phantom.connectors._
import storage.cassandra._

object TestConnector {
  val connector = ContactPoint.embedded.keySpace("test")
}

object TestDb extends CassandraStorage(TestConnector.connector)
