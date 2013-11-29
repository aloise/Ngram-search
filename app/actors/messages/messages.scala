package actors.messages

import scala.collection.immutable

trait RowAction[R]{ 
  val row:R
}
case class InsertRowAction[R](row:R) extends RowAction[R]
case class UpdateRowAction[R](row:R) extends RowAction[R]
case class DeleteRowAction[R](row:R) extends RowAction[R]
case class RemoveRowsFromIteration(iteration:Int)

case class SearchRowsRequest(search:String, limit:Int)
case class SearchRowsResult(ids:immutable.Seq[Int] = List())