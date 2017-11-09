package agh.iosr.paxos

import java.net.InetSocketAddress

import agh.iosr.paxos.predef.{IdToIpMap, IpToIdMap}
import akka.actor.ActorSystem
import akka.io.{IO, Udp}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Matchers, WordSpecLike}

case class TestMessage() extends SendableMessage

class CommunicatorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers {

  def generatePrereq(clusterIps: List[InetSocketAddress]) = {
    val ipToId: IpToIdMap = clusterIps.zipWithIndex.toMap
    val idToIp: IdToIpMap = ipToId.map(_.swap)
    (ipToId, idToIp)
  }


  "Connector" must {
    "unicast" must {
      val testActorIp = new InetSocketAddress("127.0.0.1", 9692)

      val unicastSet: List[InetSocketAddress] = List(
        testActorIp,
        new InetSocketAddress("127.0.0.1", 9971),
      )

      val (ipToId, idToIp) = generatePrereq(unicastSet)
      val comm = system.actorOf(Communicator.props(Set(self), testActorIp, ipToId, idToIp))

      "forward incoming messages to master" in {
        val data = TestMessage()
        comm ! Udp.Received(SerializationHelper.serialize(data), null)
        expectMsg(ReceivedMessage(data, predef.NULL_NODE_ID))
      }

      "send unicast messages" in {
        IO(Udp) ! Udp.Bind(self, unicastSet(1))
        expectMsg(Udp.Bound(unicastSet(1)))

        val data = TestMessage()
        comm ! SendUnicast(data, 1)
        expectMsg(Udp.Received(SerializationHelper.serialize(data), testActorIp))
      }
    }


    "send multicast messages" in {
      val testActorIp = new InetSocketAddress("127.0.0.1", 9693)

      val multicastSet: List[InetSocketAddress] = List(
        testActorIp,
        new InetSocketAddress("127.0.0.1", 9981),
        new InetSocketAddress("127.0.0.1", 9982),
        new InetSocketAddress("127.0.0.1", 9983),
      )

      val (ipToId, idToIp) = generatePrereq(multicastSet)
      val comm = system.actorOf(Communicator.props(Set(), testActorIp, ipToId, idToIp))

      val setLen = multicastSet.size
      multicastSet.slice(1, setLen).foreach {
        address =>
          IO(Udp) ! Udp.Bind(self, address)
          expectMsg(Udp.Bound(address))
      }

      val data = TestMessage()
      comm ! SendMulticast(data)

      val receivedMsg = Udp.Received(SerializationHelper.serialize(data), testActorIp)
      multicastSet.slice(1, setLen).foreach {
        _ => expectMsg(receivedMsg)
      }

    }

  }

}
