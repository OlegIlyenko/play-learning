package controllers

import play.api.mvc._
import scaldi.{Injectable, Injector}
import model.{Book, BookDao}
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.Writeable

class Books(implicit inj: Injector) extends Controller with Injectable {

  val bookDao = inject [BookDao]

  def books = Action { implicit req =>
    Ok(views.html.books.list(bookDao.findAll))
  }

  def book(id: Int) = Action { implicit req =>
    withBook(id, views.html.books.detail.apply)
  }

  def bookEdit(id: Int) = Action { implicit req =>
    withBook(id, book => views.html.books.edit(bookForm.fill(book)))
  }

  private def withBook[C](id: Int, fn: Book => C)(implicit writeable: Writeable[C]) =
    bookDao.get(id) map (book => Ok(fn(book))) getOrElse NotFound(s"Book with ID $id not found!")

  def bookAdd = Action { implicit req =>
    Ok(views.html.books.edit(bookForm))
  }

  val bookForm = Form[Book](mapping(
    "id" -> optional(number),
    "title" -> nonEmptyText,
    "author" -> nonEmptyText,
    "publishYear" -> number(min = 1900, max = 3000),
    "cool" -> boolean,
    "owners" -> list(nonEmptyText)
  )(Book.apply)(Book.unapply))
}