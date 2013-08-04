import controllers.Books
import model.{Book, SimpleBookDao, BookDao}
import play.api.GlobalSettings
import scaldi.Module
import scaldi.play.ScaldiSupport

/**
 *
 * @author Oleg Ilyenko
 */
object Global extends GlobalSettings with ScaldiSupport {
  def applicationModule = new Module {
    // controllers

    binding to new Books

    // DAO

    bind [BookDao] to new SimpleBookDao(List(
      Book(Some(1), "Book ", "me", 2013, false, Nil),
      Book(Some(2), "Programming in Scala", "Martin", 2000, true, List("Me", "Someone Else"))
    ))
  }
}
