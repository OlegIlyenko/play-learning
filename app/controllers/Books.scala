package controllers

import play.api.mvc._
import scaldi.{Injectable, Injector}
import model.{Book, BookDao}
import play.api.data.Form
import play.api.data.Forms
import Forms._
import play.api.http.Writeable
import play.api.data.validation.{Valid, Invalid, ValidationResult, Constraint}

class Books(implicit inj: Injector) extends Controller with Injectable {

  val bookDao = inject [BookDao]

  def list = Action { implicit req =>
    Ok(views.html.books.list(bookDao.findAll))
  }

  def get(id: Int) = Action { implicit req =>
    withBook(id, views.html.books.detail.apply)
  }

  def edit(id: Int) = Action { implicit req =>
    withBook(id, book => views.html.books.edit(bindOError(bookForm, bookForm fill book), Some(id)))
  }

  def delete(id: Int) = Action { implicit req =>
    bookDao.delete(id)

    Redirect(routes.Books.list()).flashing("success" -> s"Book with ID $id deleted!")
  }

  def add = Action { implicit req =>
    Ok(views.html.books.edit(bindOError(bookForm, bookForm), None))
  }

  def save(id :Int) = Action { implicit req =>
    bookForm.bindFromRequest().fold(
      hasErrors = errorRedirect(routes.Books.edit(id)),
      success = book => {
        bookDao.saveOrUpdate(book.copy(id = Some(id)))

        successRedirect(routes.Books.get(id))
      }
    )
  }

  def create = Action { implicit req =>
    bookForm.bindFromRequest().fold(
      hasErrors = errorRedirect(routes.Books.add()),
      success = book => successRedirect(routes.Books.get(bookDao.saveOrUpdate(book).id.get))
    )
  }

  private def bindOError[E, F <: Form[E], T](form: F, noErrorForm: => F)(implicit req: Request[T]) =
    flash.get("error") map (_ => form bind flash.data) getOrElse noErrorForm

  private def errorRedirect[E, F[_] <: Form[_], T](page: Call)(implicit req: Request[T]) =
    (form: F[E]) => Redirect(page).flashing(Flash(form.data) + ("error" -> "Validation Errors"))

  private def successRedirect[T](page: Call)(implicit req: Request[T]) =
    Redirect(page).flashing("success" -> "Book saved!")

  private def withBook[C](id: Int, fn: Book => C)(implicit writeable: Writeable[C]) =
    bookDao.get(id) map (book => Ok(fn(book))) getOrElse NotFound(s"Book with ID $id not found!")

  val notMe = Constraint((v: String) => if (v.equalsIgnoreCase("Me")) Invalid("You are not allowed! :D") else Valid)

  val bookForm = Form[Book](mapping(
    "id" -> optional(number),
    "title" -> nonEmptyText,
    "author" -> nonEmptyText.verifying(notMe),
    "publishYear" -> number(min = 1900, max = 3000),
    "cool" -> boolean,
    "owners" -> Forms.list(nonEmptyText)
  )(Book.apply)(Book.unapply))
}