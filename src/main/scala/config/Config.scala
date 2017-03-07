package config

object Config {
  val ZK_STRING = "localhost:2181"
  val TOPIC = "recommender-clickstream"
  val ZK_SPOUT_ID = "kafka-recommender-clickstream"

  val CASSANDRA_HOST = "192.168.3.7"
  val CASSANDRA_KEYSPACE = "product_recommender"

  val ACTION_WEIGHTS = Map(
    "display" -> 0,
    "hover" -> 1,
    "click" -> 2,
    "like" -> 3,
    "share" -> 4,
    "buy" -> 5
//    "0.5" -> 1,
//    "1.0" -> 1,
//    "1.5" -> 2,
//    "2.0" -> 2,
//    "2.5" -> 3,
//    "3.0" -> 3,
//    "3.5" -> 4,
//    "4.0" -> 4,
//    "4.5" -> 5,
//    "5.0" -> 5
  )

  val MOVIE_NAMES_FILE_LOCATION = "/var/lib/jetty/resources/movies.csv"

  val KAFKA_PRODUCER_CONFIG_LOCATION = "/kafka-0.10.0.1-producer-defaults.properties"
  val KAFKA_TOPIC_NAME = "recommender-clickstream"

  val SERVER_HOST = "0.0.0.0"
  val SERVER_PORT = 8090

  val TRENDING_ITEMS_LIST_SIZE = 100
  val RECENT_VIEWS_LIST_SIZE = 100
}
