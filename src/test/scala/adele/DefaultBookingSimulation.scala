package adele

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class DefaultBookingSimulation extends Simulation {

    val httpConf = http
            .baseURL("http://localhost:8080")
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

    val scn = scenario("Default Booking Scenario")
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
                    .body(StringBody("""{"eventId": 1, "sectorId": 1, "positions": [1, 2, 3, 4, 5]}""")).asJSON)

    setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}