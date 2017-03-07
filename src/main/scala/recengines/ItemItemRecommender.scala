package recengines

import scala.util.{Failure, Success}
import scala.math.{Ordering, min, sqrt}
import scala.concurrent.ExecutionContext.Implicits.global
import storage.cassandra.{CassandraStorage, Similarity, SimilarityIndex, User}
import config.Config
import org.apache.storm.tuple.Tuple
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future
import scala.io.Source
import storage.Event

class ItemItemRecommender(storage: CassandraStorage) {

  // for demonstration purpose only
//  val moviesNames = getMovieNames(Config.MOVIE_NAMES_FILE_LOCATION)

  implicit val eventFormat = jsonFormat6(Event)

  def getMovieNames(fileLocation: String): scala.collection.mutable.Map[String, String] = {
    val movieNames = scala.collection.mutable.Map[String, String]()
    for (line <- Source.fromFile(fileLocation).getLines() if !line.startsWith("movieID")) {
      val record = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1).map(_.stripPrefix("\"").stripSuffix("\""))
      val (movieId, title) = (record(0), record(1))
      movieNames(movieId) = title
    }
    movieNames
  }

  def trackEvent(event: Event): Unit = {
    val userId = event.userId
    val itemId = event.itemId
    val action = event.action

    if (action != "display") {
      val newWeight = Config.ACTION_WEIGHTS(action)
      val user = storage.users.getById(userId)

      user.onComplete {
        case Success(Some(u)) =>
          val currentWeight = u.items.getOrElse(itemId, 0)
          if (currentWeight >= newWeight) ()
          else {
            storage.users.updateUser(User(u.id, u.items + (itemId -> newWeight)))
            recalculateSimilarity(u, itemId, currentWeight, newWeight)
          }

        case Success(None) => saveNewUser(userId, itemId, newWeight)

        case Failure(message) => println(message)
      }
    }
  }

  def recalculateSimilarity(user: User, itemId: String, currentWeight: Int, newWeight: Int): Unit = {
    println("RECALCULATING SIMILARITY")
    val currentItemCount = storage.itemCounts.getById(itemId)

    def callback(currentItemCount: Long): Unit = {
      val itemCountDelta = newWeight - currentWeight
      updateItemCount(itemId, itemCountDelta)
      val newItemCount = currentItemCount + itemCountDelta
      for {
        anotherItem <- user.items if anotherItem._1 != itemId
      } updatePair(itemId, currentWeight, newWeight, newItemCount, anotherItem)
    }

    currentItemCount.onComplete{
      case Success(Some(item)) => callback(item.count)
      case Success(None) => callback(0)
      case Failure(message) => println(message)
    }
  }

  def updateItemCount(itemId: String, deltaWeight: Int): Unit = {
    storage.itemCounts.incrementCount(itemId, deltaWeight)
  }

  def updatePair(eventItemId: String, currentItemWeight: Int, newItemWeight: Int,
                 newItemCount: Long, anotherItem: (String, Int)): Unit = {
    val anotherItemId = anotherItem._1
    val anotherItemWeight = anotherItem._2
    updatePairCount(eventItemId, currentItemWeight, newItemWeight, newItemCount, anotherItemId, anotherItemWeight)
  }

  def getPairId(firstItemId: String, secondItemId: String): String =
    Ordering[String].min(firstItemId, secondItemId) + "_" + Ordering[String].max(firstItemId, secondItemId)

  def updatePairCount(eventItemId: String, currentItemWeight: Int, newItemWeight: Int,
                      newItemCount: Long, anotherItemId: String, anotherItemWeight: Int): Unit = {

    val deltaCoRating = {
      if (currentItemWeight == 0) min(newItemWeight, anotherItemWeight)
      else {
        (currentItemWeight < anotherItemWeight, newItemWeight < anotherItemWeight) match {
          case (true, true) => newItemWeight - currentItemWeight
          case (true, false) => anotherItemWeight - currentItemWeight
          case (false, _) => 0
        }
      }
    }

    val pairId = getPairId(eventItemId, anotherItemId)
    val currentPairCount = storage.pairCounts.getById(pairId)

    def callback(initCount: Long): Unit = {
      if (deltaCoRating != 0) {
        storage.pairCounts.incrementCount(pairId, deltaCoRating)
        updateSimilarity(eventItemId, newItemCount, anotherItemId, initCount + deltaCoRating)
      }
      else updateSimilarity(eventItemId, newItemCount, anotherItemId, initCount + deltaCoRating)
    }

    currentPairCount.onComplete{
      case Success(Some(count)) => callback(count.count)
      case Success(None) => callback(0)
      case Failure(message) => println(message)
      }
    }


  def updateSimilarity(firstItem: String, newItemCount: Long, secondItem: String, pairCount: Long): Unit = {
    val secondItemCount = storage.itemCounts.getById(secondItem)
    println("PAIRCOUNT: " + pairCount + ", NEWITEMCOUNT: " + newItemCount)
    for {
      Some(secondItem) <- secondItemCount
      similarity: Double = pairCount / (sqrt(newItemCount) * sqrt(secondItem.count))
    } saveSimilarity(firstItem, secondItem.itemId, similarity)
  }

  def saveSimilarity(firstItem: String, secondItem: String, similarity: Double): Unit = {
    println("SAVING SIMILARITY")
    val pairId = getPairId(firstItem, secondItem)
    val similarityIndex = storage.similaritiesIndex.getById(pairId)
    similarityIndex.onComplete{
      case Success(Some(currentSimilarity)) =>
        storage.similarities.deleteRow(Similarity(firstItem, secondItem, currentSimilarity.similarity))
        storage.similarities.deleteRow(Similarity(secondItem, firstItem, currentSimilarity.similarity))
        storage.similarities.store(Similarity(firstItem, secondItem, similarity))
        storage.similarities.store(Similarity(secondItem, firstItem, similarity))
        storage.similaritiesIndex.store(SimilarityIndex(pairId, similarity))

      case Success(None) =>
        storage.similarities.store(Similarity(firstItem, secondItem, similarity))
        storage.similarities.store(Similarity(secondItem, firstItem, similarity))
        storage.similaritiesIndex.store(SimilarityIndex(pairId, similarity))

      case Failure(message) => println(message)
    }
  }

  def saveNewUser(userId: String, itemId: String, weight: Int) = {
    println("SAVING NEW USER")
    storage.users.store(User(userId, Map(itemId -> weight)))
    updateItemCount(itemId, weight)
  }

  def getSimilarItems(itemId: String, limit: Int = 10): Future[Seq[Similarity]] = {
    val similarities = storage.similarities.getById(itemId, limit)
    similarities.map(simList => Similarity(itemId, itemId, 1) +: simList)
//    similarities.map(simList => (Similarity(itemId, itemId, 1) +: simList).map(sim => Map("item" -> sim.anotherItemId, "title" -> moviesNames(sim.anotherItemId), "score" -> sim.similarity)))
  }

  def getRecommendations(userId: String, limit: Int = 10): Future[Seq[(String, Double)]] = {

    type UserItem = (String, Int)

    def getUserItems(user: Option[User]): Seq[UserItem] = user match {
      case Some(u) => u.items.toList
      case None => Nil
    }

    def getItemSimilarity(userItems: Seq[UserItem]): Seq[(UserItem, Future[Seq[Similarity]])] = {
      userItems.map(userItem => (userItem, storage.similarities.getById(userItem._1, limit)))
    }

    def reformatSimilarity(similarities: Seq[(UserItem, Future[Seq[Similarity]])]): Future[Seq[(UserItem, Seq[Similarity])]] = {
      val (items, sims) = similarities.unzip
      val sims1 = Future.sequence(sims)
      sims1.map(x => items.zip(x))
    }

    def getSimilaritySummands(similarities: Seq[(UserItem, Seq[Similarity])]): Seq[(String, Double, Double)] = {
      for {
        (item, sims) <- similarities
        sim <- sims
      } yield (sim.anotherItemId, sim.similarity * item._2, sim.similarity)
    }

    def getSimilaritySum(ratingsRaw: Seq[(String, Double, Double)]): Seq[(String, Double)] = {
      ratingsRaw.groupBy(_._1).map {
        case (x, y) => (x, y.map(_._2).sum / y.map(_._3).sum)
      }.toSeq
    }

    for {
      items <- storage.users.getById(userId).map(getUserItems)
      recommendations <- reformatSimilarity(getItemSimilarity(items)).map(getSimilaritySummands).map(getSimilaritySum)
    } yield recommendations.filter(rec => !items.map(_._1).contains(rec._1)).sortWith(_._2 > _._2)
  }
}
