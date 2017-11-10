package agh.iosr.paxos

import agh.iosr.paxos.Messages._
import agh.iosr.paxos.predef._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

case class InstanceState(lastParticipated: RoundId, lastVoted: RoundId, vote: Option[KeyValue], remote: NodeId)

object Acceptor {
  def props(): Props =
    Props(new Acceptor())
}

class Acceptor()
  extends Actor with ActorLogging {

  var communicator: ActorRef = _
  var runningInstances: mutable.Map[InstanceId, InstanceState] = mutable.Map.empty
  var highestInstance: InstanceId = NULL_INSTANCE_ID

  override def receive: Receive = {
    case Ready =>
      communicator = sender()
      context.become(ready)
  }

  def ready: Receive = {
    case ReceivedMessage(data, remoteId) =>

      data match {
        case Prepare(RoundIdentifier(instanceId, roundId)) =>

          runningInstances.getOrElse(instanceId, InstanceState(NULL_ROUND, NULL_ROUND, None, NULL_NODE_ID)) match {
            case InstanceState(NULL_ROUND, _, _, _) =>
              runningInstances(instanceId) = InstanceState(roundId, NULL_ROUND, None, remoteId)
              highestInstance = math.max(highestInstance, instanceId)
              communicator ! SendUnicast(Promise(RoundIdentifier(instanceId, roundId), NULL_ROUND, None), remoteId)

            case InstanceState(lastParticipated, _, _, lastRemote)
              if roundId <= lastParticipated && remoteId != lastRemote =>
                communicator ! SendUnicast(RoundTooOld(RoundIdentifier(instanceId, roundId), highestInstance), remoteId)

            case InstanceState(lastParticipated, lastVoted, vote, lastRemote)
              if roundId > lastParticipated || (roundId == lastParticipated && remoteId == lastRemote) =>
                runningInstances(instanceId) = InstanceState(roundId, lastVoted, vote, remoteId)
                communicator ! SendUnicast(Promise(RoundIdentifier(instanceId, roundId), lastVoted, vote), remoteId)
          }

        case AcceptRequest(RoundIdentifier(instanceId, roundId), value) =>

          runningInstances.getOrElse(instanceId, InstanceState(NULL_ROUND, NULL_ROUND, None, NULL_NODE_ID)) match {
            case InstanceState(lastParticipated, lastVoted, vote, lastRemote)
              if roundId >= lastParticipated && (roundId != lastVoted || (vote.contains(value) && remoteId == lastRemote))=>
                runningInstances(instanceId) = InstanceState(roundId, roundId, Some(value), remoteId)
                highestInstance = math.max(highestInstance, instanceId)
                communicator ! SendMulticast(Accepted(RoundIdentifier(instanceId, roundId), value))

            case InstanceState(lastParticipated, _, _, _) =>
              communicator ! SendUnicast(HigherProposalReceived(RoundIdentifier(instanceId, roundId), lastParticipated), remoteId)
          }

        case FallAsleep => context.become(down)

        case _ =>
      }
  }

  def down: Receive = {
    case ReceivedMessage(WakeUp, _) => context.become(ready)

    case _ =>
  }

}