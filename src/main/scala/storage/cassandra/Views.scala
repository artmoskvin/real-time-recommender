package storage.cassandra

import scala.concurrent.Future
import com.websudos.phantom.CassandraTable
import com.websudos.phantom.dsl._
import spray.json._
import DefaultJsonProtocol._


case class ViewItem(
               itemId: String,
               timestamp: Long
               )

case class UserViews(
               userId: String,
               views: List[ViewItem]
               )

class ViewsTable extends CassandraTable[Views, UserViews] {

  implicit val itemFormat = jsonFormat2(ViewItem)
  implicit val viewFormat = jsonFormat2(UserViews)

  object userId extends StringColumn(this) with PartitionKey[String]
  object views extends JsonListColumn[ViewItem](this) {
    override def fromJson(obj: String): ViewItem = obj.parseJson.convertTo[ViewItem]

    override def toJson(obj: ViewItem): String = obj.toJson.compactPrint
  }

  def fromRow(row: Row): UserViews = {
    UserViews(
      userId(row),
      views(row)
    )
  }

}

abstract class Views extends ViewsTable with RootConnector {

  def store(userViews: UserViews): Future[ResultSet] = {
    insert.value(_.userId, userViews.userId).value(_.views, userViews.views)
      .consistencyLevel_=(ConsistencyLevel.ALL)
      .future()
  }

  def getById(userId: String): Future[Option[UserViews]] = {
    select.where(_.userId eqs userId).one()
  }

}
