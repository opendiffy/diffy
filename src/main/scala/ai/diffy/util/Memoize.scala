package ai.diffy.util

import java.util.concurrent.ConcurrentHashMap

object Memoize {
  def apply[A, B](function: A => B): A => B = {
    val map = new ConcurrentHashMap[A, B]()
    (a: A) => {
      map.putIfAbsent(a, function(a))
      map.get(a)
    }
  }
}
