package simulations
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
class ChallengeSet extends Simulation{

  val httpConf = http.baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")

  def getAllVideoGames() ={
    exec(
      http("Get all video games")
        .get("videogames")
        .check(status.is(200))
        .check(jsonPath("$[2].name").saveAs("gameId")))
        .exec { session => println(session); session}
    )
  }

  def createNewGame()={

  }

  def getSpecificVideoGame ()={
    exec(
      http("Get a video game")
        .get("videogames/${gameId}")
        .check(status.in(200 to 210))
    )
  }

  val scn = scenario("Challenge set simulation")
    .exec(getAllVideoGames())
    .pause(3)
    .exec(getSpecificVideoGame())
    .pause(3)

  setUp(
    scn.inject(
      nothingFor(5.seconds),
      constantUsersPerSec(10) during (10.seconds),
      rampUsersPerSec(1) to (5) during (20.seconds)
    ).protocols(httpConf.inferHtmlResources())
  )
}
