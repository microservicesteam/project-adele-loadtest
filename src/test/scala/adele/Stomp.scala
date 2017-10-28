package adele

import io.gatling.core.Predef.DurationInteger
import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Stomp {

    val StompConnect = "CONNECT\n" +
            "accept-version:1.1,1.0\n" +
            "heart-beat:10000,10000\n" +
            "\n" +
            "\u0000"

    val StompConnectCheck = wsListen
            .within(new DurationInteger(5).seconds)
            .until(1)
            .regex("CONNECTED")
            .saveAs("connection-response")

    val StompSubscribe = "SUBSCRIBE\n" +
            "id:sub-0\n" +
            "destination:/topic/tickets\n" +
            "\n" +
            "\u0000"

}
