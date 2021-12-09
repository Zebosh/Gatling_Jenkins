package finalSimulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.lang.System.getProperty
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.DurationInt
import scala.util.Random

class VideoGameFullTestTemplate extends Simulation {

  val httpConf = http.baseUrl("http://localhost:8080/app/")
    .header("Accept", "application/json")
    .proxy(Proxy("localhost", 8888))

  /** * HTTP CALLS ** */
  // 1. Get all games
  def getAllVideoGames() = {
    exec(
      http("Get all video games")
        .get("videogames")
        .check(status.is(200))
    )
  }

  //2. Create new Game
  def createNewGame() = {
    exec(http("Post a video game")
      .post("videogames")
      .body(ElFileBody("bodies/NewGameTemplate.json")).asJson
      .check(status.is(200)))
  }

  // 3. Get details of that single
  def getSpecificVideoGame() = {
    exec(
      http("Get a video game")
        .get("videogames/${gameId}")
        .check(status.in(200 to 210))
    )
  }

  // 4. Delete the game
  def deleteGame() = {
    exec(http("Delete a video game")
      .delete("videogames/${gameId")
      .check(status.is(200)))

  }

  /** SETUP LOAD SIMULATION */

  // create a scenario that has runtime parameters for:
  // 1. Users
  // 2. Ramp up time
  // 3. Test duration


  /** Custom Feeder */

  var idNumbers = (20 to 30).iterator
  val rnd = new Random()
  val now = LocalDate.now()
  val pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def randomString(length: Int) = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  def getRandomDate(startDate: LocalDate, random: Random): String = {
    startDate.minusDays(random.nextInt(30)).format(pattern)
  }

  val customFeeder = Iterator.continually(Map(
    "gameId" -> idNumbers.next(),
    "name" -> ("Game-" + randomString(5)),
    "releaseDate" -> getRandomDate(now, rnd),
    "reviewScore" -> rnd.nextInt(100),
    "category" -> ("Category-" + randomString(6)),
    "rating" -> ("Rating-" + randomString(4))
  ))

  val scn = scenario("Challenge set simulation")
    .exec(getAllVideoGames())
    .pause(3)
    .exec(createNewGame())
    .pause(3)
    .exec(getSpecificVideoGame())
    .pause(3)
    .exec(deleteGame())
    .pause(2)


  setUp(
    scn.inject(
      nothingFor(5.seconds),
      constantUsersPerSec(10) during (10.seconds),
      rampUsersPerSec(1) to (5) during (20.seconds)
    ).protocols(httpConf.inferHtmlResources())
  )

  // to generate the date for the Create new Game JSON

  /** Helper methods */

  // for the custom feeder, or the defaults for the runtime parameters... and anything

  /** Variables */
  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  // for the helper methods
  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  /** Before & After */
  // to print out message at the start and end of the test

  before {
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} seconds")
    println(s"Total test duration: ${testDuration} seconds")
  }

  after {
    println("Stress test completed")
  }

}
