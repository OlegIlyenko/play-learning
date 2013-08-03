package controllers

import play.api._
import play.api.mvc._
import views.html.hello
import scaldi.{Injectable, Injector}
import model.BookDao

class Application(implicit inj: Injector) extends Controller with Injectable {

  val bookDao = inject [BookDao]

  def index = Action {
    Ok("Hello my N-th play example app !11111")
  }

  def greet(times: Int) = Action {
    Ok(hello(1 to times map ("Hello user " + _) toList))
  }

  def books = Action {
    Ok(views.html.books.list(bookDao.findAll))
  }

}