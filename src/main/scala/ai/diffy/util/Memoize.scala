package ai.diffy.util

import java.util.concurrent.ConcurrentHashMap

object Memoize {
  def apply[A, B](function: A => B): A => B = {
    val map = new ConcurrentHashMap[A, B]()
    (a: A) => {
      map.computeIfAbsent(a, function.apply)
      map.get(a)
    }
  }
}
