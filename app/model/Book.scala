package model

case class Book(title: String, author: String, publishYear: Int)

trait BookDao {
  def findAll: List[Book]
}

class SimpleBookDao(books: List[Book]) extends BookDao {
  def findAll = books sortBy (_.publishYear)
}

