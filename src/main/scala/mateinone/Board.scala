package mateinone

import Rank._
import Square._

object Side {
  def apply(color: Color, pawns: Set[Square], knights: Set[Square], bishops: Set[Square], rooks: Set[Square], queens: Set[Square], kings: Set[Square]): Side =
    Side(color, (pawns.map(_ -> Pawn) ++ knights.map(_ -> Knight) ++ bishops.map(_ -> Bishop) ++ rooks.map(_ -> Rook) ++ queens.map(_ -> Queen) ++ kings.map(_ -> King)).toMap,
      Map(Pawn -> pawns, Knight -> knights, Bishop -> bishops, Rook -> rooks, Queen -> queens, King -> kings), Set.empty[Square])
}

// Provides an interface to update and query a side's pieces (color, square, type and moved).
case class Side private(color: Color,
                        private val squareToType: Map[Square, PieceType],
                        private val typeToSquares: Map[PieceType, Set[Square]],
                        private val moved: Set[Square]) {
  def `type`(s: Square): PieceType = squareToType(s)
  def squares(t: PieceType): Set[Square] = typeToSquares(t)
  def moved(s: Square): Boolean = moved.contains(s)
  def contains(s: Square): Boolean = squareToType.contains(s)
  def contains(t: Set[PieceType])(s: Square): Boolean = t.exists(typeToSquares(_).contains(s))
  def add(s: Square, t: PieceType): Side =
    copy(squareToType = squareToType + (s -> t), typeToSquares = typeToSquares + (t -> (typeToSquares(t) + s)), moved = moved + s)
  def remove(s: Square, t: PieceType): Side =
    copy(squareToType = squareToType - s, typeToSquares = typeToSquares + (t -> (typeToSquares(t) - s)))
}

object Board {

  private object OffsetConstants {
    val (up, upRight, right, downRight, down, downLeft, left, upLeft) = ((0, 1), (1, 1), (1, 0), (1, -1), (0, -1), (-1, -1), (-1, 0), (-1, 1))
    val (upOnly, downOnly) = (Vector(up), Vector(down))
    val (upLeftUpRight, downLeftDownRight) = (Vector(upLeft, upRight), Vector(downLeft, downRight))
    val (diagonals, verticalHorizontal) = (Vector(upRight, downRight, downLeft, upLeft), Vector(up, left, down, right))
    val adjacent = diagonals ++ verticalHorizontal
    val knight = Vector((2, 1), (2, -1), (1, 2), (1, -2), (-2, 1), (-2, -1), (-1, 2), (-1, -2))
  }

  private object PieceTypeConstants {
    val k: Set[PieceType] = Set(Knight)
    val rq: Set[PieceType] = Set(Rook, Queen)
    val rqk: Set[PieceType] = Set(Rook, Queen, King)
    val pbqk: Set[PieceType] = Set(Pawn, Bishop, Queen, King)
    val bqk: Set[PieceType] = Set(Bishop, Queen, King)
    val bq: Set[PieceType] = Set(Bishop, Queen)
  }

  private object SquareConstants {
    val blackSquares: Set[Square] = Square.Squares.flatten.toSet.filter(s => s.file % 2 == 0 && s.file % 2 == 0)
    val rank7: Set[Square] = rank(_7)
    val rank2: Set[Square] = rank(_2)
  }

  private case class CastleConstants(move: Castle, rookStart: Square, kingEnd: Square, between: Set[Square], through: Set[Square], rookEnd: Square)
  private val (whiteQueens, whiteKings, blackQueens, blackKings) =
    (CastleConstants(`O-O-O`, A1, C1, Set(B1, C1, D1), Set(C1, D1), D1), CastleConstants(`O-O`, H1, G1, Set(F1, G1), Set(F1), F1),
      CastleConstants(`O-O-O`, A8, C8, Set(B8, C8, D8), Set(C8, D8), D8), CastleConstants(`O-O`, H8, G8, Set(F8, G8), Set(F8), F8))

  def initial = Board(Side(White, rank(_2), Set(B1, G1), Set(C1, F1), Set(A1, H1), Set(D1), Set(E1)),
    Side(Black, rank(_7), Set(B8, G8), Set(C8, F8), Set(A8, H8), Set(D8), Set(E8)))

  private def isCheck(defender: Side, offender: Side): Boolean = {
    import OffsetConstants._
    import PieceTypeConstants._
    defender.squares(King).exists { king =>
      def givingCheck(inOneStep: Set[PieceType], inAnySteps: Set[PieceType])(offset: (Int, Int)): Boolean = {
        var c = king + offset; var g = inOneStep
        while (c != Outside && !defender.contains(c) && !offender.contains(c)) { c = c + offset; g = inAnySteps }
        c != Outside && offender.contains(g)(c)
      }
      verticalHorizontal.exists(givingCheck(rqk, rq)) || knight.exists(offset => offender.contains(k)(king + offset)) ||
        downLeftDownRight.exists(givingCheck(if (defender.color == Black) pbqk else bqk, bq)) ||
        upLeftUpRight.exists(givingCheck(if (defender.color == White) pbqk else bqk, bq))
    }
  }

}
import Board._

case class Board private(same: Side, opponent: Side) {

  def leaves: Vector[(MoveBase, Board)] = {
    import OffsetConstants._
    import SquareConstants._

    def oneStep(start: Square, offset: (Int, Int), filter: Square => Boolean): Set[Square] =
    { val end = start + offset; if (end != Outside && filter(end)) Set(end) else Set.empty[Square] }
    val openOnly1 = oneStep(_: Square, _: (Int, Int), s => !same.contains(s) && !opponent.contains(s))
    def openOnly2(start: Square, offset: (Int, Int)): Set[Square] =
    { val ones = openOnly1(start, offset); ones ++ ones.flatMap(openOnly1(_, offset)) }
    val captureOnly = oneStep(_: Square, _: (Int, Int), opponent.contains)
    val openOrCapture = oneStep(_: Square, _: (Int, Int), !same.contains(_))
    def openUntilCapture(start: Square, offset: (Int, Int)): Set[Square] = {
      var result = Set.empty[Square]; var c = start + offset
      while (c != Outside && !same.contains(c) && !opponent.contains(c)) { result = result + c; c = c + offset }
      if (c != Outside && opponent.contains(c)) result + c else result
    }

    def toMoves(start: Square, ends: Set[Square]): Set[Move] = ends.map(Move.move(start, _))
    def toPromotions(start: Square, ends: Set[Square]): Set[Promotion] =
      ends.flatMap(e => PromotionType.all.map(t => Promotion(start, e, t)))

    def captureIfPresent(s: Square): Side = if (opponent.contains(s)) opponent.remove(s, opponent.`type`(s)) else opponent
    def doMove(m: Move): Board = {
      val t = same.`type`(m.start)
      Board(captureIfPresent(m.end), same.add(m.end, t).remove(m.start, t))
    }
    def doPromotion(p: Promotion): Board =
      Board(captureIfPresent(p.end), same.remove(p.start, Pawn).add(p.end, p.`type`))

    def createLeaves[M <: MoveBase](starts: Set[Square],
                                    offsets: Vector[(Int, Int)],
                                    createEnds: (Square, (Int, Int)) => Set[Square],
                                    createMoves: (Square, Set[Square]) => Set[M],
                                    createBoard: M => Board): Vector[(M, Board)] =
      starts.toVector.flatMap(s => offsets.flatMap(o => createMoves(s, createEnds(s, o))).map(m => (m, createBoard(m))))

    (createLeaves(same.squares(Knight), knight, openOrCapture, toMoves, doMove) ++
      createLeaves(same.squares(Bishop), diagonals, openUntilCapture, toMoves, doMove) ++
      createLeaves(same.squares(Rook), verticalHorizontal, openUntilCapture, toMoves, doMove) ++
      createLeaves(same.squares(Queen), adjacent, openUntilCapture, toMoves, doMove) ++
      createLeaves(same.squares(King), adjacent, openOrCapture, toMoves, doMove) ++
      {
        val (promotionRank, advance, captures) = if (same.color == White) (rank7, upOnly, upLeftUpRight) else (rank2, downOnly, downLeftDownRight)
        val (promoters, movers) = same.squares(Pawn).partition(promotionRank.contains)
        createLeaves(promoters, advance, openOnly1, toPromotions, doPromotion) ++
          createLeaves(promoters, captures, captureOnly, toPromotions, doPromotion) ++
          createLeaves(movers.filterNot(same.moved), advance, openOnly2, toMoves, doMove) ++
          createLeaves(movers.filter(same.moved), advance, openOnly1, toMoves, doMove) ++
          createLeaves(movers, captures, captureOnly, toMoves, doMove)
      } ++
      {
        val (kingStart, kingside, queenside) = if (same.color == White) (E1, whiteKings, whiteQueens) else (E8, blackKings, blackQueens)
        var castleLeaves = Vector.empty[(Castle, Board)]
        if (!same.moved(kingStart) && !isCheck) {
          def createLeaf(cc: CastleConstants): Unit = {
            def betweenOccupied = cc.between.exists(b => same.contains(b) || opponent.contains(b))
            def throughCheck = Board.isCheck(cc.through.foldLeft(same)(_.add(_, King)), opponent)
            def board = Board(opponent, same.remove(cc.rookStart, Rook).remove(kingStart, King).add(cc.rookEnd, Rook).add(cc.kingEnd, King))
            if (!same.moved(cc.rookStart) && !betweenOccupied && !throughCheck) castleLeaves = castleLeaves :+(cc.move, board)
          }
          createLeaf(kingside); createLeaf(queenside)
        }
        castleLeaves
      }).filterNot { case (_, b) => Board.isCheck(b.opponent, b.same) }

  }

  def moves: Vector[MoveBase] = leaves.map(_._1)
  def boards: Vector[Board] = leaves.map(_._2)

  def isCheck: Boolean = Board.isCheck(same, opponent)
  def isCheckmate: Boolean = moves.isEmpty && isCheck

  def isStalemate: Boolean = moves.isEmpty && !isCheck
  def isInsufficientMaterial: Boolean = {
    import SquareConstants._
    def both(t: PieceType): Set[Square] = same.squares(t) ++ opponent.squares(t)
    (both(Pawn).isEmpty || both(Rook).isEmpty || both(Queen).isEmpty) &&
      ((both(Knight).isEmpty || both(Bishop).isEmpty) || (both(Knight).size == 1 && both(Bishop).isEmpty) ||
       (both(Knight).isEmpty && (both(Bishop).forall(blackSquares.contains) || !both(Bishop).exists(blackSquares.contains))))
  }
  def isAutomaticDraw: Boolean = isStalemate || isInsufficientMaterial

  def isThreefoldRepetition: Boolean = false
  def isFiftyMoveRule: Boolean = false
  def mayClaimDraw: Boolean = isThreefoldRepetition || isFiftyMoveRule

  def move(movesToMake: List[MoveBase]): Option[Board] = movesToMake match { case h :: t => leaves.find(_._1 == h).flatMap(_._2.move(t)); case _ => Some(this) }
  def move(movesToMake: MoveBase*): Option[Board] = move(movesToMake.toList)

}
