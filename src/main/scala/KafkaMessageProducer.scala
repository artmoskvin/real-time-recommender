import config.Config
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}



class KafkaMessageProducer {

  def initKafkaProducer[K, V]: KafkaProducer[K, V] = {
    val props = new java.util.Properties()
    val resource = this.getClass.getResourceAsStream(Config.KAFKA_PRODUCER_CONFIG_LOCATION)
    props.load(resource)
    new KafkaProducer[K, V](props)
  }

  def createKafkaMessage(event: String): Unit = {

    val message = new ProducerRecord(Config.KAFKA_TOPIC_NAME, event)
    this.kafkaProducer.send(message)

  }

  val kafkaProducer = initKafkaProducer[Nothing, String]
}
