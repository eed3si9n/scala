object Test {
  if (true) List(1, 23)
  else nil

  def nil[A]: List[A] = List.empty[A]
}
