package recengines

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import config.Config
import storage.Event
import storage.cassandra.{CassandraStorage, TrendingItemCount}


class TrendingRecommender(storage: CassandraStorage) {

  def trackEvent(event: Event): Unit = {

    val itemId = event.itemId
    val action = event.action
    val weight = Config.ACTION_WEIGHTS(action)

    val trendingItems = getRecommendations()
    trendingItems.onComplete {
      case Success(items) =>
        if (items.exists(_.itemId == itemId)) {
          val currentItemIndex = items.indexWhere(_.itemId == itemId)
          val currentScore = items(currentItemIndex).count
          storage.trendingItemCounts.deleteRow(TrendingItemCount("trending", itemId, currentScore))
          storage.trendingItemCounts.store(TrendingItemCount("trending", itemId, currentScore + weight))
        }

        else if (items.length < Config.TRENDING_ITEMS_LIST_SIZE)
          storage.trendingItemCounts.store(TrendingItemCount("trending", itemId, weight))
        else updateCounts(items, itemId, weight)

      case Failure(msg) => println(msg)
    }

  }

  def updateCounts(items: Seq[TrendingItemCount], itemId: String, weight: Int): Unit = {
    items.foreach(storage.trendingItemCounts.deleteRow)
    val adjustedCount = items.map(item => TrendingItemCount("trending", item.itemId, item.count - weight))
    val toUpdateList = adjustedCount.filter(item => item.count > 0)
    toUpdateList.foreach(storage.trendingItemCounts.store)
    if (toUpdateList.length < items.length) storage.trendingItemCounts.store(TrendingItemCount("trending", itemId, weight))
  }

  def getRecommendations(limit: Int = 100): Future[Seq[TrendingItemCount]] = {
    storage.trendingItemCounts.getAll(limit)
  }

}
