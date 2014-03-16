package mateinone

sealed trait PieceType
case object Pawn extends PieceType
case object King extends PieceType

object PromotionType { val all = Vector(Knight, Rook, Bishop, Queen) }
sealed trait PromotionType extends PieceType
case object Knight extends PromotionType
case object Rook extends PromotionType
case object Bishop extends PromotionType
case object Queen extends PromotionType

sealed trait Side { val other: Side }
case object White extends Side { val other: Side = Black }
case object Black extends Side { val other: Side = White }

case class Piece(side: Side, `type`: PieceType, square: Square, hasMoved: Boolean)
