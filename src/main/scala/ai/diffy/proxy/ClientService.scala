package ai.diffy.proxy

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.finagle.{Addr, Name, Service}
import com.twitter.util.{Time, Var}

trait ClientService[Req, Rep] {
  val client: Service[Req, Rep]
}

case class ThriftService(
    override val client: Service[ThriftClientRequest, Array[Byte]],
    path: Name)
  extends ClientService[ThriftClientRequest, Array[Byte]]
{
  var members: Int = 0
  var serversetValid = false
  var changedAt: Option[Time] = None
  var resetAt: Option[Time] = None
  var changeCount: Int = 0

  val boundServerset: Option[Var[Addr]] =
    path match {
      case Name.Bound(addr) =>
        serversetValid = true
        Some(addr)
      case _ =>
        serversetValid = false
        None
    }

  boundServerset foreach {
    _.changes.respond {
      case Addr.Bound(addrs, _) =>
        serversetValid = true
        sizeChange(addrs.size)
      case Addr.Failed(_) | Addr.Neg =>
        serversetValid = false
        sizeChange(0)
      case Addr.Pending => ()
    }
  }

  private[this] def sizeChange(size: Int) {
    changeCount += 1
    if (changeCount > 1) {
      changedAt = Some(Time.now)
      if (members == 0) {
        resetAt = Some(Time.now)
      }
    }
    members = size
  }
}

case class HttpService(
  override val client: Service[Request, Response])
extends ClientService[Request, Response]
