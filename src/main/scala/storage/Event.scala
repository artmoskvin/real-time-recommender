package storage

case class Event (
                   userId: String,
                   itemId: String,
                   action: String,
                   timestamp: Long,
                   recommendationId: Option[String],
                   price: Option[Double]
                 )
