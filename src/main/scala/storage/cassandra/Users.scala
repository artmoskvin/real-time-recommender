package storage.cassandra

import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._

import scala.concurrent.Future

case class User(
                 id: String,
                 items: Map[String, Int]
               )

class UsersTable extends CassandraTable[Users, User] {

  object id extends StringColumn(this) with PartitionKey[String]
  object items extends MapColumn[String, Int](this)

  def fromRow(row: Row): User = {
    User(
      id(row),
      items(row)
    )
  }
}

abstract class Users extends UsersTable with RootConnector {

  def store(user: User): Future[ResultSet] = {
    insert.value(_.id, user.id).value(_.items, user.items)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def updateUser(user: User): Future[ResultSet] = {
    update.where(_.id eqs user.id).modify(_.items setTo user.items).future()
  }

  def getById(id: String): Future[Option[User]] = {
    select.where(_.id eqs id).one()
  }
}
