package ai.diffy.examples.thrift

import java.net.InetSocketAddress

import ai.diffy.thriftscala._
import com.twitter.finagle.ThriftMux
import com.twitter.util.Future

object ExampleServers {
  def main(args: Array[String]): Unit = {
    val primary = args(0).toInt
    val secondary = args(1).toInt
    val candidate = args(2).toInt

    val baseline = new AdderExample({case (a:Int,b:Int) => a + b})
    val sut = new AdderExample({case (a:Int,b:Int) => a * b})

    ThriftMux.server.serveIface(new InetSocketAddress(primary), baseline)
    ThriftMux.server.serveIface(new InetSocketAddress(secondary), baseline)
    ThriftMux.server.serveIface(new InetSocketAddress(candidate), sut)
  }
}

class AdderExample(f: (Int,Int) => Int) extends Adder.MethodPerEndpoint {
  override def add(a:Int,b:Int)= Future.value(f(a, b))
}
