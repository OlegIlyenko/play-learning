package model

case class Book(id: Option[Int], title: String, author: String, publishYear: Int, cool: Boolean, owners: List[String])

trait BookDao {
  def findAll: List[Book]
  def get(id: Int): Option[Book]
}

class SimpleBookDao(books: List[Book]) extends BookDao {
  def findAll = books sortBy (_.publishYear)

  def get(id: Int) = books find (_.id == Some(id))
}

