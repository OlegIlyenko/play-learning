import akka.actor.{Props, ActorRef}
import controllers.Books
import model._
import model.Book
import model.Comment
import org.joda.time.{DateTime, LocalDate}
import play.api.{Play, GlobalSettings}
import scala.Some
import scaldi.Module
import scaldi.play.ScaldiSupport
import play.api.libs.concurrent.Akka._

object Global extends GlobalSettings with ScaldiSupport {
  def applicationModule = new Module {
    // controllers

    binding to new Books

    // DAO

    bind [BookDao] to new SimpleBookDao(List(
      Book(Some(1), "Book ", "me", 2013, cool = false, Nil),
      Book(Some(2), "Programming in Scala", "Martin", 2000, cool = true, List(Comment(DateTime.now, "Me")))
    ))

    bind [ActorRef] identifiedBy 'comments to
      system(Play.current).actorOf(Props(new CommentsRouter))
  }
}
