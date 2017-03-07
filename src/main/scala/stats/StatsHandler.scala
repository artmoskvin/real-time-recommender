package stats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import storage._
import cassandra._

class StatsHandler(storage: CassandraStorage) {

  def trackEvent(event: Event): Unit = {

    val userId = event.userId
    val itemId = event.itemId
    val action = event.action
    val timestamp = event.timestamp
    val datetime = new DateTime(timestamp)
    val recommendationId = event.recommendationId.getOrElse("")
    val price = event.price.getOrElse(0.0)

    storage.stats.store(Stat(action, userId, itemId, datetime, recommendationId, price))

  }

  def formatRate(n: Double): String = "%.2f".format(n)

  def groupStatByPeriod(stats: Seq[Stat], timestampPattern: String): Map[String, Long] = {
    stats
        .map(stat => (stat.timestamp.toString(timestampPattern), 1))
        .groupBy(_._1).mapValues(_.map(_._2).sum)
  }

  def getEventCountsGroupedByPeriod(event: String, from: DateTime, to: DateTime, period: String,
                                    recommendation: Boolean = false): Future[Map[String, Long]] = {

    val events = if (recommendation) storage.stats.getEventsFromRecommendation(event, from, to)
                 else storage.stats.getEvents(event, from, to)

    period match {
      case "minutely" => events.map(seq => groupStatByPeriod(seq, "yyyy-MM-dd'T'HH:mm"))
      case "hourly" => events.map(seq => groupStatByPeriod(seq, "yyyy-MM-dd'T'HH"))
      case "daily" => events.map(seq => groupStatByPeriod(seq, "yyyy-MM-dd"))
      case "weekly" => events.map(seq => groupStatByPeriod(seq, "yyyy-ww"))
      case "monthly" => events.map(seq => groupStatByPeriod(seq, "yyyy-MM"))
      case "yearly" => events.map(seq => groupStatByPeriod(seq, "yyyy"))
      case _ => Future(Map())
    }
                 
  }

  def calculateThroughRate(target: Map[String, Long], base: Map[String, Long]): Map[String, String] = {
    for (k <- base.keys)
      yield k -> formatRate(target.getOrElse(k, 0L) / base(k).toDouble)
  }.toMap

  def getThroughRateByPeriod(target: String, base: String,
                             from: DateTime, to: DateTime,
                             period: String, recommendation: Boolean = false): Future[Map[String, String]] = {

    val targets = getEventCountsGroupedByPeriod(target, from, to, period, recommendation)
    val bases = getEventCountsGroupedByPeriod(base, from, to, period, recommendation)

    for {
      t <- targets
      b <- bases
    } yield calculateThroughRate(t, b)

  }

  def getCtrGroupedByPeriod(from: DateTime, to: DateTime,
                            period: String, recommendation: Boolean = false): Future[Map[String, String]] = {

    getThroughRateByPeriod("click", "display", from, to, period, recommendation)

  }

  def getLtrGroupedByPeriod(from: DateTime, to: DateTime,
                            period: String, recommendation: Boolean = false): Future[Map[String, String]] = {

    getThroughRateByPeriod("like", "click", from, to, period, recommendation)

  }

  def getStrGroupedByPeriod(from: DateTime, to: DateTime,
                            period: String, recommendation: Boolean = false): Future[Map[String, String]] = {

    getThroughRateByPeriod("share", "click", from, to, period, recommendation)

  }

  def getBtrGroupedByPeriod(from: DateTime, to: DateTime,
                            period: String, recommendation: Boolean = false): Future[Map[String, String]] = {

    getThroughRateByPeriod("buy", "click", from, to, period, recommendation)

  }

  def groupSalesByPeriod(stats: Seq[Stat], timestampPattern: String): Map[String, Double] = {
    stats
      .map(stat => (stat.timestamp.toString(timestampPattern), stat.price))
      .groupBy(_._1).mapValues(_.map(_._2).sum)
  }
  
  def getSalesGroupedByPeriod(from: DateTime, to: DateTime, period: String,
                              recommendation: Boolean = false): Future[Map[String, Double]] = {

    val sales = if (recommendation) storage.stats.getSalesFromRecommendation(from, to)
                else storage.stats.getSales(from, to)

    period match {
      case "minutely" => sales.map(seq => groupSalesByPeriod(seq, "yyyy-MM-dd'T'HH:mm"))
      case "hourly" => sales.map(seq => groupSalesByPeriod(seq, "yyyy-MM-dd'T'HH"))
      case "daily" => sales.map(seq => groupSalesByPeriod(seq, "yyyy-MM-dd"))
      case "weekly" => sales.map(seq => groupSalesByPeriod(seq, "yyyy-ww"))
      case "monthly" => sales.map(seq => groupSalesByPeriod(seq, "yyyy-MM"))
      case "yearly" => sales.map(seq => groupSalesByPeriod(seq, "yyyy"))
      case _ => Future(Map())
    }

  }

  def calculateSalesRate(totalSales: Map[String, Double], recommenderSales: Map[String, Double]): Map[String, Map[String, String]] = {

    for {
      k <- totalSales.keys
      tS = totalSales(k)
      rS = recommenderSales.getOrElse(k, 0.0)
    } yield k -> Map("total" -> formatRate(tS), "recommender" -> formatRate(rS), "rate" -> formatRate(rS / tS))

  }.toMap

  def getSalesRatesGroupedByPeriod(from: DateTime, to: DateTime, period: String): Future[Map[String, Map[String, String]]] = {

    val totalSales = getSalesGroupedByPeriod(from, to, period, recommendation = false)
    val recommenderSales = getSalesGroupedByPeriod(from, to, period, recommendation = true)

    for {
      tS <- totalSales
      rS <- recommenderSales
    } yield calculateSalesRate(tS, rS)

  }

}
