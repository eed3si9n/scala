object Test {
  if (true) List(1, 23)
  else nil

  def nil[A]: List[A] = List.empty[A]

  // fall back to lubing for existential types
  def ancestry(clazz: Class[_]): List[Class[_]] =
    if (clazz == classOf[AnyRef] || !classOf[AnyRef].isAssignableFrom(clazz)) List(clazz)
    else clazz :: ancestry(clazz.getSuperclass)

  // fall back to lubing for existential types
  trait Settings[ScopeType] {
    sealed case class ScopedKey[T](scope: ScopeType, key: Option[T]) {
    }
    final class Compiled[T](
      val key: ScopedKey[T],
      val dependencies: Iterable[ScopedKey[_]]
    )
    type CompiledMap = Map[ScopedKey[_], Compiled[_]]
    val compiled: CompiledMap = Map.empty
    val locals = compiled flatMap {
      case (key, comp) =>
        if (key.key.isDefined) Seq[Compiled[_]](comp)
        else Seq[Compiled[_]]()
    }
  }

  // fall back to lubing for existential types
  def combinedLax(x: Int, any: Option[_]): Option[String] =
    (any orElse (Option(x): Option[_])).map(_.toString)
}
