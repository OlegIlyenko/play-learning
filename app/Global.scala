import _root_.play.api.libs.concurrent.Akka
import akka.actor.{Props, ActorRef}
import controllers.Books
import model._
import model.Book
import model.Comment
import org.joda.time.DateTime
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.GraphDatabaseService
import _root_.play.api.{Application, Play, GlobalSettings}
import scala.Some
import scaldi._
import scaldi.play.ScaldiSupport
import scaldi.play.condition._

object Global extends GlobalSettings with ScaldiSupport {
  def db(typ: String) = Condition(inject[String]("db.type") == typ)

  lazy val neo4j = db("embedded-neo4j") and !inTestMode

  def applicationModule = new Module {
    // controllers

    binding to new Books

    bind [ActorRef] identifiedBy 'comments to Akka.system(Play.current).actorOf(Props(new CommentsRouter))

    // DAO

    bind [BookDao] when !neo4j to new SimpleBookDao(List(
      Book(Some(1), "Management 3.0", "Jurgen Appelo", 2011, cool = true, Nil),
      Book(Some(2), "Programming in Scala", "Martin Odersky", 2011, cool = true, List(Comment(DateTime.now, "Me")))
    ))

    bind [GraphDatabaseService] when neo4j to
      new GraphDatabaseFactory().newEmbeddedDatabase(inject [String] ("db.embedded-neo4j.path"))

    bind [BookDao] when neo4j to new Neo4jBookDao ~ (_ createInitialGraphIfNewDb)
  }

  override def onStop(app: Application) {
    if(neo4j satisfies Nil) {
      inject[GraphDatabaseService].shutdown()
    }
  }
}