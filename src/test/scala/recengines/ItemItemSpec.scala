package recengines

import scala.concurrent.ExecutionContext.Implicits.global
import org.specs2.mutable._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.specification.BeforeAfterAll
import storage._
import cassandra._


class ItemItemSpec extends Specification with BeforeAfterAll with TestConnector.connector.Connector {

  sequential

  type EE = ExecutionEnv

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
      recommender.trackEvent(event)
      Thread.sleep(1000)
    }

  }

  def afterAll(): Unit = {
    TestDb.autotruncate.future()
  }

  val testDb = TestDb
  val recommender = new ItemItemRecommender(testDb)


  "Item-Item Recommender correctly" >> {

    "tracks events" >> {

      "for item i1" >> { implicit ee: EE =>
        val similarity = testDb.similarities.getById("i1")
        similarity must beEqualTo(Seq(Similarity("i1", "i4", 0.8040302522073697), Similarity("i1", "i2", 0.36363636363636365))).await
      }

      "for item i2" >> { implicit ee: EE =>
        val similarity = testDb.similarities.getById("i2")
        similarity must beEqualTo(Seq(Similarity("i2","i3",0.6708203932499369), Similarity("i2","i5",0.5477225575051661), Similarity("i2","i1",0.36363636363636365), Similarity("i2","i4",0.30151134457776363))).await
      }

      "for item i3" >> { implicit ee: EE =>
        val similarity = testDb.similarities.getById("i3")
        similarity must beEqualTo(Seq(Similarity("i3","i2",0.6708203932499369), Similarity("i3","i5",0.6123724356957945))).await
      }

      "for item i4" >> { implicit ee: EE =>
        val similarity = testDb.similarities.getById("i4")
        similarity must beEqualTo(Seq(Similarity("i4", "i1", 0.8040302522073697), Similarity("i4", "i2", 0.30151134457776363))).await
      }

      "for item i5" >> { implicit ee: EE =>
        val similarity = testDb.similarities.getById("i5")
        similarity must beEqualTo(Seq(Similarity("i5", "i3", 0.6123724356957945), Similarity("i5", "i2", 0.5477225575051661))).await
      }
    }


    "gives recommendations" >> {

      "for user u1" >> { implicit ee: EE =>
        val recommendations = recommender.getRecommendations("u1")
        recommendations must beEqualTo(Seq(("i5", 3), ("i3", 3))).await
      }

      "for user u2" >> { implicit ee: EE =>
        val recommendations = recommender.getRecommendations("u2")
        recommendations must beEqualTo(Seq(("i4", 4), ("i1", 4))).await
      }

      "for user u3" >> { implicit ee: EE =>
        val recommendations = recommender.getRecommendations("u3")
        recommendations must beEqualTo(Seq(("i2", 2))).await
      }

      "for user u4" >> { implicit ee: EE =>
        val recommendations = recommender.getRecommendations("u4")
        recommendations must beEqualTo(Seq(("i4", 3), ("i1", 2.9999999999999996))).await
      }

      "for user u5" >> { implicit ee: EE =>
        val recommendations = recommender.getRecommendations("u5")
        recommendations must beEqualTo(Seq(("i5", 1), ("i3", 1))).await
      }

    }

  }

}
