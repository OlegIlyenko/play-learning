package controllers

import play.api.mvc._
import scaldi.{Injectable, Injector}
import model.{Comment, Book, BookDao}
import play.api.data.Form
import play.api.data.Forms
import Forms._
import play.api.http.Writeable
import play.api.data.validation.{Valid, Invalid, Constraint}
import play.api.i18n.Messages
import play.api.libs.json.JsValue
import akka.actor.ActorRef
import model.CommentActionMessage.Join
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import play.api.libs.iteratee.{Enumerator, Iteratee}

class Books(implicit inj: Injector) extends Controller with Injectable {

  val bookDao = inject [BookDao]
  val comments = inject [ActorRef] ('comments)

  implicit val timeout = Timeout(5.seconds)

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

    Redirect(routes.Books.list()).flashing("success" -> Messages("message.deleted", id))
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

  def commentSocket(id: Int) = WebSocket.async[JsValue] { implicit req =>
    (comments ? Join(id)).mapTo[(Iteratee[JsValue, _], Enumerator[JsValue])]
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
    "comments" -> ignored[List[Comment]](Nil)
  )(Book.apply)(Book.unapply))
}