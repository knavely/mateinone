package mateinone

sealed trait PieceType

object PromotionType { val all = Set(Knight, Rook, Bishop, Queen) }
sealed trait PromotionType extends PieceType

case object Pawn extends PieceType
case object King extends PieceType
case object Knight extends PromotionType
case object Rook extends PromotionType
case object Bishop extends PromotionType
case object Queen extends PromotionType

sealed trait Side
case object White extends Side
case object Black extends Side

case class Piece(side: Side, pieceType: PieceType, square: Square, hasMoved: Boolean) {
  def atEnd(move: Move): Piece = copy(square = move.end, hasMoved = true)
  def promotedTo(promotionType: PromotionType): Piece = copy(pieceType = promotionType)
}
