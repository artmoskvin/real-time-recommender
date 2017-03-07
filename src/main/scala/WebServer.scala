import scala.concurrent.Future
import scala.io.StdIn
import com.github.nscala_time.time.Imports._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import recengines.{CounterRecommender, ItemItemRecommender, TrendingRecommender}
import spray.json.DefaultJsonProtocol._
import spray.json._
import storage.Event
import storage.cassandra.{CassandraStorage, Similarity, TrendingItemCount, ViewItem}
import config.Config
import stats.StatsHandler


object WebServer {

  final case class SimilaritiesRecommendation(id: String, recommendation: Seq[Similarity])
  final case class ItemsRecommendation(id: String, recommendation: Seq[(String, Double)])
  final case class CounterRecommendation(id: String, recommendation: Seq[(String, Long)])
  final case class RecentViews(id: String, recommendation: List[ViewItem])

  implicit val eventFormat = jsonFormat6(Event)
  implicit val viewItemFormat = jsonFormat2(ViewItem)
  implicit val similarityFormat = jsonFormat3(Similarity)
  implicit val similaritiesRecommendationFormat = jsonFormat2(SimilaritiesRecommendation)
  implicit val itemsRecommendationFormat = jsonFormat2(ItemsRecommendation)
  implicit val counterRecommendationFormat = jsonFormat2(CounterRecommendation)
  implicit val recentViewsFormat = jsonFormat2(RecentViews)


  def uuid = java.util.UUID.randomUUID.toString

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    implicit val executionContext = system.dispatcher

    val storage = CassandraStorage
    val itemItemRecommender = new ItemItemRecommender(storage)
    val trendingRecommender = new TrendingRecommender(storage)
    val counterRecommender = new CounterRecommender(storage)
    val stats = new StatsHandler(storage)

    val kafkaProducer = new KafkaMessageProducer

    val route: Route =
      get {
        pathPrefix("item" / """\w+""".r) { id =>
          parameter('limit.as[Int] ? 100) { limit =>
            val similarItems: Future[Seq[Similarity]] = itemItemRecommender.getSimilarItems(id, limit)

            onSuccess(similarItems) {
              sim => complete(SimilaritiesRecommendation(uuid, sim))
            }
          }
        } ~
        pathPrefix("user" / """\w+""".r) { id =>
          parameter('limit.as[Int] ? 100) { limit =>
            val recommendedItems: Future[Seq[(String, Double)]] = itemItemRecommender.getRecommendations(id, limit)

            onSuccess(recommendedItems) {
              rec => complete(ItemsRecommendation(uuid, rec))
            }
          }

        } ~
        pathPrefix("trending") { parameter('limit.as[Int] ? 100) { limit =>
            val trendingItems: Future[Seq[(String, Long)]] = trendingRecommender.getRecommendations(limit).map(seq => seq.map(item => (item.itemId, item.count)))
            onSuccess(trendingItems) {
              rec => complete(CounterRecommendation(uuid, rec))
            }
          }
        } ~
        pathPrefix("bestsellers") { parameter('limit.as[Int] ? 100) { limit =>
            val bestSellers: Future[Seq[(String, Long)]] = counterRecommender.getBestSellers(limit).map(seq => seq.map(item => (item.itemId, item.count)))
            onSuccess(bestSellers) {
              rec => complete(CounterRecommendation(uuid, rec))
            }
          }
        } ~
        pathPrefix("recentviews" / """\w+""".r) { userId =>
          val recentViews: Future[List[ViewItem]] = counterRecommender.getRecentViews(userId)
          onSuccess(recentViews) {
            rec => complete(RecentViews(uuid, rec))
          }
        } ~
        pathPrefix("stats") {
          pathPrefix("sales") { parameters('from.as[Long], 'to.as[Long], 'period) { (from, to, period) =>
              val fromDate = new DateTime(from)
              val toDate = new DateTime(to)

              val sales: Future[Map[String, Map[String, String]]] = stats.getSalesRatesGroupedByPeriod(fromDate, toDate, period)

            onSuccess(sales) {
                rec => complete(rec)
              }
            }
          } ~
          pathPrefix("ctr") { parameters('recommendation ? true, 'from.as[Long], 'to.as[Long], 'period) { (rec, from, to, period) =>
              val fromDate = new DateTime(from)
              val toDate = new DateTime(to)

              val ctr: Future[Map[String, String]] = stats.getCtrGroupedByPeriod(fromDate, toDate, period, rec)

              onSuccess(ctr) {
                rec => complete(rec)
              }
            }
          } ~
          pathPrefix("ltr") { parameters('recommendation ? true, 'from.as[Long], 'to.as[Long], 'period) { (rec, from, to, period) =>
              val fromDate = new DateTime(from)
              val toDate = new DateTime(to)

              val ltr: Future[Map[String, String]] = stats.getLtrGroupedByPeriod(fromDate, toDate, period, rec)

              onSuccess(ltr) {
                rec => complete(rec)
              }
            }
          } ~
          pathPrefix("str") { parameters('recommendation ? true, 'from.as[Long], 'to.as[Long], 'period) { (rec, from, to, period) =>
              val fromDate = new DateTime(from)
              val toDate = new DateTime(to)

              val str: Future[Map[String, String]] = stats.getStrGroupedByPeriod(fromDate, toDate, period, rec)

              onSuccess(str) {
                rec => complete(rec)
              }
            }
          } ~
          pathPrefix("btr") { parameters('recommendation ? true, 'from.as[Long], 'to.as[Long], 'period) { (rec, from, to, period) =>
              val fromDate = new DateTime(from)
              val toDate = new DateTime(to)

              val btr: Future[Map[String, String]] = stats.getBtrGroupedByPeriod(fromDate, toDate, period, rec)

              onSuccess(btr) {
                rec => complete(rec)
              }
            }
          }
        }
      } ~
      post {
        pathPrefix("learn") {
          entity(as[Event]) { event =>
            kafkaProducer.createKafkaMessage(event.toJson.compactPrint)
            complete("Message successfully sent to Kafka")
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, Config.SERVER_HOST, Config.SERVER_PORT)
    println(s"Server online at http://${Config.SERVER_HOST}:${Config.SERVER_PORT}/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ ⇒ system.terminate()) // and shutdown when done

  }
}
