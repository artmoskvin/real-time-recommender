# Description
Real-time recommender system for marketplace or any other media platform.

The coolest thing about it is that it's fully real-time. This means that there are milliseconds latency
between receiving some event and presenting recalculated recommendations.

Almost every modern real-time data processing pipeline consists of 3 core elements: event log, streaming engine and NoSQL
storage. The first one helps you collect events in fast and secure way, the second one processes them and the results
are stored into the third one. Here we use [Apache Kafka][1], [Apache Storm][2] and [Apache Cassandra][3].
We receive events (i.e. user clickstream) in Kafka, process them in Storm and store results in Cassandra.
You can find an overview in Architecture section.

To sent an event into this pipeline you simply POST it as a JSON to one of the API points. To GET the actual recommendations
you make a call to another point. You can find more details in How to use section.

To recalculate recommendations in real-time there has to be suitable algorithm. It should be lightweight and
perfectly scalable. This product recommender supports different algorithms for different use cases. All of them are described
in details in Recommenders section.

# Architecture
![Product Recommender Architecture.png](https://bitbucket.org/repo/gdEkXx/images/2392421889-Product%20Recommender%20Architecture.png)
# Recommenders
At the moment Product Recommender supports following recommenders:

* Collaborative Filtering Item-Item
* Trending Items

## Collaborative Filtering Item-Item Recommender
Collaborative filtering (CF) is well-known as one of the best algorithm for personalized recommendations. CF tries to recommend
items based on user behaviour in the past. CF is based on calculating similarity score and that calculation is pretty
heavy-weight. However there is an approach suggested by Tencent which turns this algorithm into real-time without
any trade-off in accuracy. Detailed description can be found here: net.pku.edu.cn/~cuibin/Papers/2015SIGMOD-tencentRec.pdf
In very brief it says that similarity score which CF is based on consists of just 3 aggregates. And instead of
aggregating them every time we can just store them and simply increment for every new event received. With this
recommendations are recalculated within few milliseconds and they are 100%-accurate.

The same whitepaper suggests solution for so called implicit feedback problem. CF algorithm is based on ratings which
user puts to different items. But often we don't have explicit ratings however we still can score user behaviour implicitly.
For example, we can say that looking at some item for 3 seconds scores 1 point, clicking on it scores 2, liking it scores 3,
sharing scores 4 and purchase scores 5 points. As a result an input for product recommender is a tuple with user ID,
item ID and score.

## Trending Items Recommender
While CF is about personalized experience, we also need to know what's trending on the marketplace overall. As our
recommender system is real-time we need to know what's trending now. To implement that we use algorithm similar to
the one used for Twitter trends.

We store top N list of items based on their aggregated scores. For every new item we check if it is present in the list.
If it is, we increment its count and it optionally goes higher in the list. If it's not, we decrement all the other
items present in a list by the score from the event. This approach makes the list very dynamic reflecting what's trending
now and not what was trending last hour or last day or last week.

# Deploy
Product Recommender is based on Kafka, Storm, Cassandra and [Docker][4]. So before you start deploying make sure that you have them
running locally or remotely. If you're new to any of these technologies, please start with reading their documentation
to understand how it works. Problems with deploy often are caused by incorrect usage or configuration of any of these
services.

If you need a shortcut, here are the exact tutorials about deploying these services: [Kafka][5], [Storm][6], [Cassandra][7],
[Docker][8].

Now when all the required services up and running you need 2 things to deploy Product Recommender:

* Build and run Docker container with web server to receive events and serve recommendations
* Submit Storm topology to process events

Both steps are sequenced linearly in the following step-by-step guide:

1. Change Kafka connection info in `resources`
2. Change Zookeeper, Cassandra and Recommender server hosts in `config.Config`
2. Run `assembly` in `sbt` to create a fat jar (if you want to skip tests, run `sbt 'set test in assembly := {}' assembly`)
3. Submit Storm topology by running `$STORM_HOME/bin/storm jar target/recommender-processor-assembly-1.0.jar storm.Topology` command in Terminal
4. Create Docker container by running `./init build` command in Terminal (optionally, use `-t` for tagging your image, default is `product_recommender`)
5. Run Docker container by running `./init run` command in Terminal (optionally, use `-p` for specifying port and `-n` for naming you container, defaults are `8090` and `product_recommender`)
6. To stop Docker container run `./init stop -n product_recommender`(or some other name you specified when running) command in Terminal
7. To remove Docker container run `./init rm -n product_recommender`(or some other name you specified when running) command in Terminal

# How to use
## Receiving events
Product recommender listens to events generated by user's clickstream. Specifically it receives
HTTP POST requests at `/learn` and transforms them into Kafka messages. Payload for a POST request should be in JSON format
and include the following keys:

    {
        "userId": "some_user",
         "itemId": "some_item",
         "action": "click",
         "timestamp": 1482744482112,
         "recommendationId": "some_id",
         "price": 9.99
    }

For every user's interaction with item there must be event sent to recommender. So `userId`, `itemId`, `action` and
`timestamp` fields are required. `timestamp` is Unix timestamp in milliseconds, in Scala can be obtained by calling
`System.currentTimeMillis()`. `recommendationId` and `price` fields are optional. If user interacts with item from
recommendation then `recommendationId` must be set. If the `action = "buy"` then `price` must be set.

Please find below the full list of possible actions:

* display
* hover
* click
* like
* share
* buy

## Serving recommendations
### Similar items
GET `/item/$ITEM_ID`
Returns similar items as JSON sorted by descending similarity score.
You can add `limit` parameter to query (default `limit = 10`).
Example:

    {
         "id": "132",
         "recommendation":
             "[
               {
                 "itemId": "i2",
                 "anotherItemId": "i2",
                 "similarity": 1
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i6",
                 "similarity": 0.9999999999999998
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i7",
                 "similarity": 0.9999999999999998
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i8",
                 "similarity": 0.9999999999999998
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i4",
                 "similarity": 0.6324555320336759
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i3",
                 "similarity": 0.4472135954999579
               },
               {
                 "itemId": "i2",
                 "anotherItemId": "i1",
                 "similarity": 0.33806170189140655
               }
             ]"
    }

### Recommendations for user
GET `/user/$USER_ID`
Returns recommended items as JSON sorted by descending predicted rating.
You can add `limit` parameter to query (default `limit = 10`).
Example:

    {
        "id": "123",
        "recommendation":
            "[
              [
                "i4",
                5
              ],
              [
                "i8",
                5
              ],
              [
                "i3",
                4
              ],
              [
                "i7",
                4
              ],
              [
                "i2",
                3
              ],
              [
                "i6",
                2
              ]
            ]"
    }

### Trending items
GET `/trending`
Returns trending items as JSON sorted by descending trending score.
You can add `limit` parameter to query (default `limit = 100`).
Example:

    {
        "id": "123",
        "recommendation":
            "[
              [
                "i2",
                29
              ],
              [
                "i1",
                17
              ],
              [
                "i6",
                12
              ],
              [
                "i7",
                10
              ],
              [
                "i8",
                10
              ]
            ]"
    }

### Bestsellers
GET `/bestsellers`
Returns list of bestseller items as JSON sorted by descending sales amount.
You can add `limit` parameter to query (default `limit = 100`).
Example:

    {
        "id": "123",
        "recommendation":
            "[
              [
                "i2",
                534
              ],
              [
                "i6",
                212
              ],
              [
                "i7",
                211
              ],
              [
                "i8",
                203
              ],
              [
                "i1",
                134
              ]
            ]"
    }

### Recent views
GET `/recentviews/$USER_ID`
Returns list of user's recently viewed items as JSON sorted by descending timestamp.
Example:

    {
        "id": "123".
        "recommendation":
            "[
                  {
                    "itemId": "i6",
                    "timestamp": 1482467944858
                  },
                  {
                    "itemId": "i1",
                    "timestamp": 1482467941858
                  },
                  {
                    "itemId": "i4",
                    "timestamp": 1482467941858
                  },
                  {
                    "itemId": "i2",
                    "timestamp": 1482406935392
                  }
            ]"
    }

## Stats, Metrics

The primary goal of any recommender is to increase sales. That is why the core metric that should be maximized here
is the sales from recommender and its share in total sales. To get bigger picture we also check some auxiliary metrics
such as CTR(click-through-rate calculated as `clicks / displays`), LTR(like-through-rate calculated as `likes / clicks`),
STR(share-through-rate calculated as `shares / clicks`) and BTR(buy-through-rate calculated as `purchases / clicks`).
To calculate these numbers we log every event into Cassandra table.

How do we distinguish user actions caused by simple browsing from actions caused by recommendation display? For every
call to product recommender along with recommendations themselves there will be recommendation `id`. For every event
caused by recommendation display `recommendationId` should be filled in. For example, if user clicks on one of the items
in Recommended block, product recommender should receive event with `recommendationId` filled in.

There is UI for visualizing these metrics. The frontend part is currently not implemented. However the backend part
(API calls) is ready. You can find mockups with API calls description [here][9].

# Contacts
Artem Moskvin moscowart99@gmail.com

[1]: https://kafka.apache.org/ "Apache Kafka"
[2]: http://storm.apache.org/ "Apache Storm"
[3]: http://cassandra.apache.org/ "Apache Cassandra"
[4]: https://www.docker.com/ "Docker"
[5]: https://kafka.apache.org/quickstart "Apache Kafka Quickstart"
[6]: http://storm.apache.org/releases/1.0.2/Setting-up-a-Storm-cluster.html "Setting up Storm cluster"
[7]: http://cassandra.apache.org/doc/latest/getting_started/installing.html "Installing Cassandra"
[8]: https://www.docker.com/products/docker "Getting Started with Docker"
[9]: https://drive.google.com/drive/folders/0B3TgTQciirlhbDhleC1ZcHRadE0?usp=sharing "Metrics UI mockups"
