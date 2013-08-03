import controllers.Application
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

    binding to new Application

    // DAO

    bind [BookDao] to new SimpleBookDao(0 to 100 flatMap (c => List(
      Book("Book " + c, "me", 2013),
      Book("Prohgramming in Scala", "Martin", 2000 + c)
    )) toList)
  }
}
