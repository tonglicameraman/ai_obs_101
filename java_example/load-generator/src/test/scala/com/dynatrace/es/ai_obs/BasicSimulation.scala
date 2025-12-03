package com.dynatrace.es.ai_obs

import com.dynatrace.es.ai_obs.BasicSimulation.TRACE_ID_HEADER
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt
import scala.util.Try

object BasicSimulation {
  val TRACE_ID_HEADER = "traceId"
}

class BasicSimulation extends Simulation {
  // Reference: https://docs.gatling.io/guides/passing-parameters/
  val TEST_DURATION: Int = Option(System.getenv("TEST_DURATION"))
    .flatMap(str => Try(str.toInt).toOption)
    .getOrElse(1)
  val TARGET_SERVER: String =
    Option(System.getenv("TARGET_SERVER")).getOrElse("localhost:8080")

  java.lang.System.err.println(
    s"Running test for $TEST_DURATION minutes against server '$TARGET_SERVER'"
  )

  // Define HTTP configuration
  // Reference: https://docs.gatling.io/reference/script/protocols/http/protocol/
  private val httpProtocol = http
    .baseUrl(s"http://$TARGET_SERVER")
    .acceptHeader("application/json")
    .userAgentHeader(
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
    )

  // Define scenario
  // Reference: https://docs.gatling.io/reference/script/core/scenario/
  private val scenario1 = scenario("Scenario 1 - Just a normal user")
    .feed(PromptProvider.NormalUserProvider.feeder)
    .exec(
      http("Prompt")
        .get("/api/v1/completion")
        .queryParamMap(
          Map(
            PromptProvider.PROMPT_PARAMETER_KEY -> s"#{${PromptProvider.PROMPT_PARAMETER_KEY}}",
            PromptProvider.FRAMEWORK_PARAMETER_KEY -> s"#{${PromptProvider.FRAMEWORK_PARAMETER_KEY}}"
          )
        )
    )

  private val scenario2 =
    scenario("Scenario 2 - Just a normal user with negative feedback")
      .feed(PromptProvider.NormalUserProvider.feeder)
      .exec(
        http("Prompt")
          .get("/api/v1/completion")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> s"#{${PromptProvider.PROMPT_PARAMETER_KEY}}",
              PromptProvider.FRAMEWORK_PARAMETER_KEY -> s"#{${PromptProvider.FRAMEWORK_PARAMETER_KEY}}"
            )
          )
          .check(jsonPath("$.traceId").saveAs(TRACE_ID_HEADER)),
        http("Feedback")
          .get("/api/v1/thumbsDown")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> s"#{${PromptProvider.PROMPT_PARAMETER_KEY}}",
              TRACE_ID_HEADER -> s"#{${TRACE_ID_HEADER}}"
            )
          ),
        http("Prompt")
          .get("/api/v1/completion")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> "Sydney",
              PromptProvider.FRAMEWORK_PARAMETER_KEY -> "rag"
            )
          )
          .check(jsonPath("$.traceId").saveAs(TRACE_ID_HEADER)),
        http("Feedback")
          .get("/api/v1/thumbsDown")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> "Sydney",
              TRACE_ID_HEADER -> s"#{${TRACE_ID_HEADER}}"
            )
          ),
        http("Prompt")
          .get("/api/v1/completion")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> "Bali",
              PromptProvider.FRAMEWORK_PARAMETER_KEY -> "rag"
            )
          )
          .check(jsonPath("$.traceId").saveAs(TRACE_ID_HEADER)),
        http("Feedback")
          .get("/api/v1/thumbsDown")
          .queryParamMap(
            Map(
              PromptProvider.PROMPT_PARAMETER_KEY -> "Bali",
              TRACE_ID_HEADER -> s"#{${TRACE_ID_HEADER}}"
            )
          )
      )

  private val scenario3 = scenario("Scenario 3 - Prompt Hacker")
    .feed(PromptProvider.HackerProvider.feeder)
    .exec(
      http("Prompt")
        .get("/api/v1/completion")
        .queryParamMap(
          Map(
            PromptProvider.PROMPT_PARAMETER_KEY -> s"#{${PromptProvider.PROMPT_PARAMETER_KEY}}",
            PromptProvider.FRAMEWORK_PARAMETER_KEY -> s"#{${PromptProvider.FRAMEWORK_PARAMETER_KEY}}"
          )
        )
    )

  //  private val scenario4 = scenario("Scenario 4 - Using the RAG with corrupted data")

  // Define assertions
  // Reference: https://docs.gatling.io/reference/script/core/assertions/
  private val assertion = global.failedRequests.count.lt(1)

  // Define injection profile and execute the test
  // Reference: https://docs.gatling.io/reference/script/core/injection/
  setUp(
    scenario1.inject(constantUsersPerSec(2) during (TEST_DURATION minutes)),
    scenario2.inject(
      constantUsersPerSec(1 / 5f) during (TEST_DURATION minutes)
    ),
    scenario3.inject(
      constantUsersPerSec(2 / 60f) during (TEST_DURATION minutes)
    )
  ).assertions(assertion).protocols(httpProtocol)
}
