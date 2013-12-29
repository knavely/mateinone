package mateinone

object Square {
  def offset(s: Square, f: Int, r: Int): Option[Square] =
    File.offset(s.file, f).flatMap(fo => Rank.offset(s.rank, r).map(ro => Square(fo, ro)))

  private def forFile(file: File) = List.fill(7)(1).scanLeft(Square(file, `1`))((s, _) => offset(s, 0, 1).get)
  val List(a1, a2, a3, a4, a5, a6, a7, a8) = forFile(A)
  val List(b1, b2, b3, b4, b5, b6, b7, b8) = forFile(B)
  val List(c1, c2, c3, c4, c5, c6, c7, c8) = forFile(C)
  val List(d1, d2, d3, d4, d5, d6, d7, d8) = forFile(D)
  val List(e1, e2, e3, e4, e5, e6, e7, e8) = forFile(E)
  val List(f1, f2, f3, f4, f5, f6, f7, f8) = forFile(F)
  val List(g1, g2, g3, g4, g5, g6, g7, g8) = forFile(G)
  val List(h1, h2, h3, h4, h5, h6, h7, h8) = forFile(H)
  def fileRank: List[List[Square]] = List(
    List(a1, a2, a3, a4, a5, a6, a7, a8),
    List(b1, b2, b3, b4, b5, b6, b7, b8),
    List(c1, c2, c3, c4, c5, c6, c7, c8),
    List(d1, d2, d3, d4, d5, d6, d7, d8),
    List(e1, e2, e3, e4, e5, e6, e7, e8),
    List(f1, f2, f3, f4, f5, f6, f7, f8),
    List(g1, g2, g3, g4, g5, g6, g7, g8),
    List(h1, h2, h3, h4, h5, h6, h7, h8)
  )
}

case class Square(file: File, rank: Rank)
