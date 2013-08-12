package model

import org.joda.time.DateTime

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

