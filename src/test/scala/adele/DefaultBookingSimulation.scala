package adele

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.collection.parallel.mutable.ParHashMap
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class DefaultBookingSimulation extends Simulation {

    private final val Random = new Random(1)

    private final val HttpBaseUrl = "http://localhost:8080"
    private final val WsBaseUrl = "ws://localhost:8080/ws"

    private final val MinimumNumberOfBookedTickets = 2
    private final val MaximumNumberOfBookedTickets = 5
    private final val NumberOfSectors = 50
    private final val SectorSize = 250

    // Init ticket map
    private final val TicketMap: ParHashMap[Int, Int] = ParHashMap()
    (1 to NumberOfSectors).foreach(sector => TicketMap += (sector -> 0))

    val httpConf = http
            .disableWarmUp // no GET request for http://gatling.io in the beginning
            .perUserNameResolution // needed if additional instances are added during load test
            .baseURL(HttpBaseUrl)
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

    val feeder = Iterator.continually(
        Map(
            "REQUEST" -> generateRequest()
        )
    )

    val defaultBooking = scenario("Default Booking Scenario")
            .feed(feeder)

            // Home Page
            .exec(http("HTTP get_events").get("/rs/api/events"))
            .pause(5)

            // First Event selected
            .exec(ws("WS Open")
                .open(WsBaseUrl))
            .pause(1)
            .exec(ws("WS Send connect")
                    .sendText(Stomp.StompConnect)
                    .check(Stomp.StompConnectCheck))
            .pause(1)
            .exec(ws("WS Send subscribe")
                    .sendText(Stomp.StompSubscribe))

            .exec(http("HTTP get_events").get("/rs/api/events"))
            .exec(http("HTTP get_venue").get("/rs/api/events/1/venue"))
            .exec(http("HTTP get_sectors").get("/rs/api/venues/1/sectors"))
            .exec(http("HTTP get_bookings").get("/bookings?eventId=1"))
            .pause(10)

            // Book selected tickets
            .exec(http("HTTP booking_request")
                    .post("/bookings")
                    .body(StringBody("""${REQUEST}""")).asJSON
                    .check(jsonPath("$.bookingId").optional.saveAs("bookingId")))
            .pause(5)

            // TODO listen to WS Message (current version does not support it)

            .exec(ws("WS Close").close)

            // Pay the tickets

    setUp(
        defaultBooking.inject(rampUsers(4000) over new DurationInt(5).minutes).protocols(httpConf)
    ).assertions(
        global.responseTime.max.lt(2000),
        global.successfulRequests.percent.is(100)
    )

    def generateRequest(): String = {
        val sector = generateSector()
        val positions = generatePositions(sector)

        "{\"eventId\": 1, \"sectorId\": " + sector + ", \"positions\": [" + positions.mkString(",") + "]}"
    }

    def generateSector(): Int = {
        var sector = 0
        var maxTry = 10

        do {
            sector = Random.nextInt(NumberOfSectors) + 1
            maxTry -= 1
        } while (TicketMap(sector) >= SectorSize || maxTry <= 0)

        sector
    }

    /**
      * Returns a Random range of positions based on the minimum and maximum number of tickets to be booked
      * e.g.: Range(5, 6, 7, 8)
      */
    def generatePositions(sector: Int): Range = {
        val numberOfTickets = math.max(MinimumNumberOfBookedTickets, Random.nextInt(MaximumNumberOfBookedTickets + 1))
        val startingPosition = TicketMap(sector) + 1
        val endPosition = math.min(startingPosition + numberOfTickets - 1, SectorSize)
        TicketMap += (sector -> endPosition)
        startingPosition to endPosition
    }

}