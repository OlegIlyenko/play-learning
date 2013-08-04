package controllers

import play.api.mvc.{Action, Controller}
import play.api.Play

object Application extends Controller {

  private val OptionMethods = List("GET", "POST", "PUT", "DELETE")

  def index = Action {
    Redirect(routes.Books.list)
  }

  def options(path: String) = Action { implicit req =>
    val routes = Play.current.routes.get
    val supportedMethods = methods.filter(m => routes.handlerFor(req.copy(method = m)).isDefined)

    if (supportedMethods.nonEmpty)
      Ok(supportedMethods mkString "\n")
    else
      NotFound
  }
}
