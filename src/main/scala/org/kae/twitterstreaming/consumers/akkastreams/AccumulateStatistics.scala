package org.kae.twitterstreaming.consumers.akkastreams

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler, TimerGraphStageLogic}
import org.kae.twitterstreaming.statistics.{StatisticsAccumulator, StatisticsSnapshot}
import org.kae.twitterstreaming.streamcontents.TweetDigest

/**
 * A custom stateful linear [[GraphStage]] that a absorbs a stream of
 * [[TweetDigest]]s derived from a Twitter stream, and emits a stream of
 * [[StatisticsSnapshot]]s at a configurable output cadence.
 *
 * @param periodBetweenOutputs time between outouts
 */
class AccumulateStatistics (periodBetweenOutputs: FiniteDuration)
  extends GraphStage[FlowShape[TweetDigest, StatisticsSnapshot]] {

  private val in = Inlet[TweetDigest]("AccumulateStatistics.in")
  private val out = Outlet[StatisticsSnapshot]("AccumulateStatistics.out")

  override val shape: FlowShape[TweetDigest, StatisticsSnapshot] = FlowShape.of(in, out)

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  override def createLogic(attr: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) {

      private val accumulator = new StatisticsAccumulator(Instant.now)

      var timerSet = false

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            // First time through set the timer for scheduling outputs
            if (! timerSet) {
              timerSet = true
              schedulePeriodically((), periodBetweenOutputs)
            }

            // Absorb the new datum and immediately transmit demand upstream.
            accumulator.accumulate(grab(in))
            pull(in)
          }
        })

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit =
            // Transmit demand upstream if we can.
            if (!hasBeenPulled(in)) {
              pull(in)
            }
        }
      )
      override def onTimer(timerKey: Any): Unit = {
        // Time to emit stats if we can.
        if (isAvailable(out)) {
          push(out, accumulator.snapshot)
        }
      }
    }
}
