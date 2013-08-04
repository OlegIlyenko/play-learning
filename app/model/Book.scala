package model

case class Book(id: Option[Int], title: String, author: String, publishYear: Int, cool: Boolean, owners: List[String])

trait BookDao {
  def findAll: List[Book]
  def get(id: Int): Option[Book]
  def saveOrUpdate(book: Book): Book
  def delete(id: Int): Unit
}

class SimpleBookDao(initialBooks: List[Book]) extends BookDao {
  var books = initialBooks

  def findAll = books sortBy (_.publishYear)

  def get(id: Int) = books find (_.id == Some(id))

  def saveOrUpdate(book: Book) =
    if (book.id.isDefined) {
      books = books.updated(books.indexWhere(_.id == book.id), book)

      book
    } else {
      val maxId = books flatMap (_.id) max
      val withId = book.copy(id = Some(maxId + 1))

      books = books :+ withId

      withId
    }

  def delete(id: Int) =
    books = books filterNot (_.id == Some(id))
}

