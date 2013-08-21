package model

import org.joda.time.DateTime
import scaldi.{Injectable, Injector}
import org.neo4j.graphdb._
import scala.Some
import org.neo4j.cypher.ExecutionEngine

case class Comment(date: DateTime, comment: String)

case class Book(id: Option[Int], title: String, author: String, publishYear: Int, cool: Boolean, comments: List[Comment])

trait BookDao {
  def findAll: List[Book]
  def get(id: Int): Option[Book]
  def saveOrUpdate(book: Book): Book
  def delete(id: Int): Unit
  def addComment(id: Int, comment: Comment): Unit
}

class SimpleBookDao(initialBooks: List[Book]) extends BookDao {
  var books = initialBooks

  def findAll = books sortBy (_.publishYear)

  def get(id: Int) = books find (_.id == Some(id))

  def saveOrUpdate(book: Book) =
    if (book.id.isDefined) {
      val oldBook = findById(book.id.get)
      books = books.updated(books indexOf oldBook, book copy (comments = oldBook.comments))

      book
    } else {
      val maxId = books flatMap (_.id) max
      val withId = book.copy(id = Some(maxId + 1))

      books = books :+ withId

      withId
    }

  def addComment(id: Int, comment: Comment): Unit = {
    val book = findById(id)

    books = books.updated(books indexOf book, book copy (comments = comment +: book.comments))
  }

  def delete(id: Int) =
    books = books filterNot (_.id == Some(id))

  private def findById(id: Int) =
    books.find(_.id == Some(id)) getOrElse (throw new IllegalStateException(s"Oooops... very strange, but book does not exist: $id"))
}

trait Neo4jSupport extends Injectable {
  implicit val inj: Injector
  private val db = inject[GraphDatabaseService]

  def withTx[T](fn: (GraphDatabaseService, Transaction) => T): T = {
    val tx = db.beginTx()
    try {
      val res = fn(db, tx)

      tx.success()

      res
    } finally {
      tx.finish()
    }
  }

  def withTx[T](fn: GraphDatabaseService => T): T = withTx((db, tx) => fn(db))

  implicit def strToRel(s: String): RelationshipType = new RelationshipType {
    def name = s.toUpperCase
  }

  implicit class BetterNode[T <: PropertyContainer](n: T) {
    def withProp(name: String, value: String) = {
      n.setProperty(name, value)
      n
    }
  }

  protected lazy val engine = new ExecutionEngine(db)
}

class Neo4jBookDao(implicit val inj: Injector) extends BookDao with Neo4jSupport {

  def createInitialGraphIfNewDb {
    withTx { db =>
      val firstNode = db.createNode withProp ("message", "Hello, ")
      val secondNode = db.createNode withProp ("message", "World!")

      val relationship = firstNode createRelationshipTo (secondNode, "knows")
      relationship setProperty("message", "brave Neo4j ")
    }

    val res = engine.execute("""
      start a = node(*)
      match (a) -[:KNOWS]-> (b)
      where has(a.message)
      return a.message + b.message as Foo
    """)

    println(res.dumpToString)
  }

  private val dummy = Book(Some(1), "Stub", "Dummy", 2011, cool = false, Nil)

  def findAll = List(dummy)

  def get(id: Int) = Some(dummy)

  def saveOrUpdate(book: Book) = dummy

  def delete(id: Int) {}

  def addComment(id: Int, comment: Comment) {}
}