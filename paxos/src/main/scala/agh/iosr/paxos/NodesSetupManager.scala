package agh.iosr.paxos

import agh.iosr.paxos.predef.{IdToIpMap, NodeId}
import akka.actor.{ActorSystem, Props}

import scala.collection.mutable

class NodesSetupManager {

  var nodes: mutable.Map[NodeId, ActorSystem] = mutable.Map.empty

  def setup(idToIpMap: IdToIpMap): Unit = {
    val ipToIdMap = idToIpMap.map(_.swap)

    idToIpMap.foreach {
      case (id, address) =>
        val system = ActorSystem("Node" + id)
        val acceptor = system.actorOf(Props(new Acceptor()))
        val learner = system.actorOf(Props(new LearnerActor()))
        val communicator = system.actorOf(Communicator.props(Set(acceptor, learner), address, ipToIdMap, idToIpMap))
        nodes += (id -> system)
    }
  }

  def terminate(idToIpMap: IdToIpMap): Unit = {
    idToIpMap.foreach {
      case (id, _) => nodes(id).terminate()
    }
  }
}
