package recengines

import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.mutable._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.BeforeAfterAll
import storage._
import cassandra._


class CounterSpec extends Specification with BeforeAfterAll with TestConnector.connector.Connector {

  sequential

  type EE = ExecutionEnv

  def trackTestEvent(event: Event): Unit = {
    recommender.trackEvent(event)
    Thread.sleep(1000)
  }

  def beforeAll(): Unit = {
    TestDb.autocreate.future()
    Thread.sleep(10000)
  }

  def afterAll(): Unit = {
    TestDb.autotruncate.future()
  }

  val testDb = TestDb
  val recommender = new CounterRecommender(testDb)

  "Counter recommender correctly" >> {
    "counts best sellers" >> { implicit ee: EE =>
      val events = Event("u1", "i1", "buy", System.currentTimeMillis() / 1000, None, None) ::
                  Event("u1", "i2", "buy", System.currentTimeMillis() / 1000, None, None) ::
                  Event("u1", "i2", "share", System.currentTimeMillis() / 1000, None, None) ::
                  Event("u1", "i2", "buy", System.currentTimeMillis() / 1000, None, None) ::
                  Nil
      events.foreach(trackTestEvent)

      val bestSellers = recommender.getBestSellers()
      val bestSellersIndex1 = TestDb.bestSellersIndex.getById("i1")
      val bestSellersIndex2 = TestDb.bestSellersIndex.getById("i2")

      (bestSellers must contain(exactly(BestSeller("bestseller", "i1", 1), BestSeller("bestseller", "i2", 2))).await) and
        (bestSellersIndex1 must beSome(BestSellerIndex("i1", 1)).await) and
        (bestSellersIndex2 must beSome(BestSellerIndex("i2", 2)).await) and
        (bestSellers.map(seq => seq.head) must beEqualTo(BestSeller("bestseller", "i2", 2)).await)

    }

    "stores recent views for user" >> { implicit ee: EE =>
      val timestamp1 = System.currentTimeMillis() / 1000
      val timestamp2 = System.currentTimeMillis() / 1000
      val events = Event("u1", "i1", "click", timestamp1, None, None) ::
        Event("u1", "i2", "click", timestamp1, None, None) ::
        Event("u1", "i3", "click", timestamp1, None, None) ::
        Event("u1", "i4", "click", timestamp1, None, None) ::
        Event("u1", "i1", "click", timestamp2, None, None) ::
        Nil
      events.foreach(trackTestEvent)

      val recentViews = recommender.getRecentViews("u1")
      (recentViews must contain(exactly(ViewItem("i1", timestamp2),
        ViewItem("i2", timestamp1), ViewItem("i3", timestamp1), ViewItem("i4", timestamp1))).await) and
        (recentViews.map(seq => seq.head) must beEqualTo(ViewItem("i1", timestamp2)).await)
    }
  }

}
