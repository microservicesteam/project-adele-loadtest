package adele

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

class DefaultBookingSimulation extends Simulation {

    private final val BaseUrl = "http://localhost:8080"

    private final val MinimumNumberOfBookedTickets = 2
    private final val MaximumNumberOfBookedTickets = 10
    private final val NumberOfSectors = 50
    private final val SectorSize = 250

    val httpConf = http
            .baseURL(BaseUrl)
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

    val feeder = Iterator.continually(
        Map(
            "SECTOR" -> generateSector(),
            "POSITIONS" -> generatePositions().mkString(",")
        )
    )

    val scn = scenario("Default Booking Scenario")
            .feed(feeder)
            // Home Page
            .exec(http("get_events").get("/rs/api/events"))
            .pause(5)
            // First Event selected
            // TODO WS connection
            .exec(http("get_events").get("/rs/api/events"))
            .exec(http("get_venue").get("/rs/api/events/1/venue"))
            .exec(http("get_sectors").get("/rs/api/venues/1/sectors"))
            .exec(http("get_bookings").get("/bookings?eventId=1"))
            .pause(10)
            // Book selected tickets
            .exec(http("booking_request")
                    .post("/bookings")
                    .body(StringBody("""{"eventId": 1, "sectorId": ${SECTOR}, "positions": [${POSITIONS}]}""")).asJSON)

    setUp(scn.inject(rampUsers(2000) over new DurationInt(2).minutes).protocols(httpConf))

    def generateSector(): Int = {
        Random.nextInt(NumberOfSectors) + 1
    }

    /**
      * Returns a random range of positions based on the minimum and maximum number of tickets to be booked
      * e.g.: Range(5, 6, 7, 8)
      */
    def generatePositions(): Range = {
        val r = Random
        val numberOfTickets = math.max(MinimumNumberOfBookedTickets, r.nextInt(MaximumNumberOfBookedTickets + 1))
        val startingPosition = r.nextInt(SectorSize - numberOfTickets + 1)
        val endPosition = startingPosition + numberOfTickets - 1
        startingPosition to endPosition
    }

}