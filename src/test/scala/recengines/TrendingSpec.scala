package recengines

import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.mutable._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.BeforeAfterAll
import storage._
import cassandra._


class TrendingSpec extends Specification with BeforeAfterAll with TestConnector.connector.Connector {

  sequential

  type EE = ExecutionEnv

  def trackTestEvent(event: Event): Unit = {
    recommender.trackEvent(event)
    Thread.sleep(1000)
  }

  def beforeAll(): Unit = {

    TestDb.autocreate.future()
    Thread.sleep(10000)
    val events = Event("u1", "i1", "buy", System.currentTimeMillis / 1000, None, None) ::
      Event("u1", "i2", "like", System.currentTimeMillis / 1000, None, None) ::
      Event("u1", "i4", "click", System.currentTimeMillis / 1000, None, None) ::
      Event("u2", "i2", "share", System.currentTimeMillis / 1000, None, None) ::
      Event("u2", "i3", "like", System.currentTimeMillis / 1000, None, None) ::
      Event("u2", "i5", "hover", System.currentTimeMillis / 1000, None, None) ::
      Event("u3", "i1", "click", System.currentTimeMillis / 1000, None, None) ::
      Event("u3", "i4", "click", System.currentTimeMillis / 1000, None, None) ::
      Event("u4", "i2", "like", System.currentTimeMillis / 1000, None, None) ::
      Event("u4", "i3", "buy", System.currentTimeMillis / 1000, None, None) ::
      Event("u4", "i5", "click", System.currentTimeMillis / 1000, None, None) ::
      Event("u5", "i1", "share", System.currentTimeMillis / 1000, None, None) ::
      Event("u5", "i2", "hover", System.currentTimeMillis / 1000, None, None) ::
      Event("u5", "i4", "buy", System.currentTimeMillis / 1000, None, None) ::
      Nil

    for (event <- events) {
      trackTestEvent(event)
    }

  }

  def afterAll(): Unit = {
    TestDb.autotruncate.future()
  }

  val testDb = TestDb
  val recommender = new TrendingRecommender(testDb)

  "Trending recommender correctly" >> {

    "increments score if item is present" >> { implicit ee: EE =>
      trackTestEvent(Event("u1", "i5", "share", System.currentTimeMillis / 1000, None, None))
      val trendingItems = testDb.trendingItemCounts.getAll()
      trendingItems must contain(TrendingItemCount("trending", "i5", 7)).await
    }

    "adds item if it is not present and there is free space" >> { implicit ee: EE =>
      trackTestEvent(Event("u1", "i6", "buy", System.currentTimeMillis / 1000, None, None))
      val trendingItems = testDb.trendingItemCounts.getAll()
      trendingItems must contain(TrendingItemCount("trending", "i6", 5)).await
    }

    "adds item after it is not present and there is no free space " +
      "but other items' counts are decreased and some of them are kicked out" >> { implicit ee: EE =>
      for (i <- 1 to 99) {
        val random = scala.util.Random
        val randomUserId = "u" + random.nextInt(5)
        val randomItemId = "i" + i
        trackTestEvent(Event(randomUserId, randomItemId, "buy", System.currentTimeMillis / 1000, None, None))
      }
      trackTestEvent(Event("u5", "i100", "hover", System.currentTimeMillis / 1000, None, None))
      trackTestEvent(Event("u1", "i101", "click", System.currentTimeMillis / 1000, None, None))
      val trendingItems = testDb.trendingItemCounts.getAll()
      (trendingItems must contain(TrendingItemCount("trending", "i101", 2)).await) and
        (trendingItems must not(contain(TrendingItemCount("trending", "i100", 1))).await)
    }

    "decreases other items' counts but doesn't add item " +
      "if it's score is not high enough or there's no free space" >> { implicit ee: EE =>
      trackTestEvent(Event("u5", "i102", "hover", System.currentTimeMillis / 1000, None, None))
      val trendingItems = testDb.trendingItemCounts.getAll()
      (trendingItems must contain(TrendingItemCount("trending", "i101", 1)).await) and
        (trendingItems must not(contain(TrendingItemCount("trending", "i102", 1))).await)
    }

  }

}
