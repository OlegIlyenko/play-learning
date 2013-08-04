package ui.model

import play.api.mvc.Call
import controllers.routes

case class Button(name: String, call: Call, icon: Option[String] = None)

object Button {
  val addBook = Button("Add Book", routes.Books.bookAdd(), Some("plus"))
  def editBook(id: Int) = Button("Edit Book", routes.Books.bookEdit(id), Some("pencil"))
}
