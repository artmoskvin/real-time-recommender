package stats


// At the moment these tests will fail because current version of phantom-sbt does not support restricting of
// non-indexed columns. However it works with production phantom. Please follow the updates of phantom-sbt and
// uncomment spec when it becomes supported.


//import scala.concurrent.ExecutionContext.Implicits.global
//import org.specs2.mutable._
//import org.specs2.concurrent.ExecutionEnv
//import org.specs2.specification.BeforeAfterAll
//import com.github.nscala_time.time.Imports._
//import storage._
//import cassandra._
//import config.Config


//class StatsHandlerSpec extends Specification with BeforeAfterAll with TestConnector.connector.Connector {
//
//  sequential
//
//  type EE = ExecutionEnv
//
//  val random = scala.util.Random
//  val actions = Config.ACTION_WEIGHTS.keys.toList
//  val testDb = TestDb
//  val handler = new StatsHandler(testDb)
//
//  def trackTestEvent(event: Event): Unit = {
//    handler.trackEvent(event)
//    Thread.sleep(1000)
//  }
//
//  def beforeAll(): Unit = {
//    TestDb.autocreate.future()
//    Thread.sleep(10000)
//  }
//
//  def afterAll(): Unit = {
//    TestDb.autotruncate.future()
//  }
//
//
//  "Stats handler correcly" >> {
//
//    val timestampFrom = new DateTime(System.currentTimeMillis() - 4 * 1000 * 60 * 60)
//    val timestampTo = new DateTime(System.currentTimeMillis() + 4 * 1000 * 60 * 60)
//
//    "calculates hourly Sales rates" >> { implicit ee: EE =>
//
//      val events = Event("userId", "itemId", "buy", 1482744482112L, Some("recId"), Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482744482113L, None, Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482744482114L, None, Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482748082112L, Some("recId"), Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482748082113L, Some("recId"), Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482748082114L, None, Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482748082115L, None, Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482751682112L, Some("recId"), Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482751682113L, Some("recId"), Some(1.0)) ::
//                   Event("userId", "itemId", "buy", 1482751682114L, None, Some(1.0)) ::
//                   Nil
//
//      events.foreach(trackTestEvent)
//
//      val expectedSalesRates = Map(
//        "2016-12-26T16" -> Map(
//          "total" -> handler.formatRate(3.0),
//          "recommender" -> handler.formatRate(1.0),
//          "rate" -> handler.formatRate(1.0 / 3.0)
//        ),
//        "2016-12-26T17" -> Map(
//          "total" -> handler.formatRate(4.0),
//          "recommender" -> handler.formatRate(2.0),
//          "rate" -> handler.formatRate(2.0 / 4.0)
//        ),
//        "2016-12-26T18" -> Map(
//          "total" -> handler.formatRate(3.0),
//          "recommender" -> handler.formatRate(2.0),
//          "rate" -> handler.formatRate(2.0 / 3.0)
//        )
//      )
//
//      val salesRates = handler.getSalesRatesGroupedByPeriod(timestampFrom, timestampTo, "hourly")
//
//      salesRates must beEqualTo(expectedSalesRates).await
//
//    }
//
//    "calculates hourly CTR" >> { implicit ee: EE =>
//
//      val events = Event("userId", "itemId", "click", 1482744482112L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482744482113L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482744482114L, Some("recId"), None) ::
//        Event("userId", "itemId", "click", 1482748082112L, Some("recId"), None) ::
//        Event("userId", "itemId", "click", 1482748082113L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482748082114L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482748082115L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482751682112L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482751682113L, Some("recId"), None) ::
//        Event("userId", "itemId", "display", 1482751682114L, Some("recId"), None) ::
//        Nil
//
//      events.foreach(trackTestEvent)
//
//      val expectedCtr = Map(
//        "2016-12-26T16" -> handler.formatRate(1 / 2.0),
//        "2016-12-26T17" -> handler.formatRate(2 / 2.0),
//        "2016-12-26T18" -> handler.formatRate(0 / 3.0)
//      )
//
//      val ctr = handler.getCtrGroupedByPeriod(timestampFrom, timestampTo, "hourly", recommendation = true)
//
//      ctr must beEqualTo(expectedCtr).await
//
//    }
//  }
//
//}
