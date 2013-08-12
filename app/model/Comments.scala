package model

import scaldi.{Injectable, Injector}
import akka.actor.Actor
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsObject
import java.util.UUID

object CommentActionMessage {
  type Message = JsValue
  case class Viewer[T](iteratee: Iteratee[Message, Unit], enumerator: Enumerator[Message], channel: Channel[Message])

  case class Join(id: Int)
  case class Leave(id: Int, viewerId: String)
  case class WelcomeViewer(id: Int, viewerId: String)
  case class AddComment(id: Int, comment: Comment)
  case class ErrorHappen(id: Int, viewerId: String, message: String)
}

object CommentInfoMessage {
  case class CommentAdded(comment: Comment)
  case class ViewerUpdate(count: Int)
  case class CommentList(comments: List[Comment])
  case class Error(message: String)

  import Json.writes

  implicit class WritesExtra[T](writes: Writes[T]) {
    def withType(tpe: String) =
      writes.transform (_.asInstanceOf[JsObject] ++ Json.obj("type" -> tpe))
  }

  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val commentFormat = Json.format[Comment]

  implicit val commentListWrites = writes[CommentList] withType "list"
  implicit val commentAddedWrites = writes[CommentAdded] withType "added"
  implicit val viewersUpdateWrites = writes[ViewerUpdate] withType "viewers"
  implicit val errorWrites = writes[Error] withType "error"
}

class CommentsRouter(implicit inj: Injector) extends Actor with Injectable {
  import model.CommentActionMessage._
  import model.CommentInfoMessage._

  val bookDao = inject [BookDao]
  var viewerCount: Map[Int, Int] = Map.empty.withDefault(_ => 0)

  var viewers : Map[Int, Map[String, Viewer[Message]]] =
    Map.empty.withDefault(_ => Map.empty)

  def receive = {
    case Join(id) =>
      val (enumerator, channel) = Concurrent.broadcast[Message]
      val viewerId = UUID.randomUUID.toString
      val iteratee = createIteratee(id, viewerId)

      viewerCount = viewerCount.updated(id, viewerCount(id) + 1)
      viewers = viewers.updated(id, viewers(id) + (viewerId -> Viewer(iteratee, enumerator, channel)))

      sender ! (iteratee, enumerator)
      self ! WelcomeViewer(id, viewerId)
    case ErrorHappen(id, viewerId, message) =>
      push(viewers(id)(viewerId).channel, Error(message))
    case AddComment(id, comment) =>
      bookDao addComment (id, comment)
      broadcast(id, CommentAdded(comment))
    case WelcomeViewer(id, viewerId) =>
      broadcast(id, ViewerUpdate(viewerCount(id)))
      push(viewers(id)(viewerId).channel, CommentList(bookDao.get(id).get.comments))
    case Leave(id, viewerId) =>
      viewerCount = viewerCount.updated(id, viewerCount(id) - 1)
      viewers = viewers.updated(id, viewers(id).filterNot(_._1 == viewerId))

      broadcast(id, ViewerUpdate(viewerCount(id)))
  }

  def createIteratee(id: Int, viewerId: String) = Iteratee.foreach[JsValue] { json =>
    json.validate[Comment] match {
      case JsSuccess(comment, _) =>
        self ! AddComment(id, comment)
      case JsError(errors) =>
        self ! ErrorHappen(id, viewerId, s"Strange Message: $errors")
    }
  }.mapDone { _ =>
    self ! Leave(id, viewerId)
  }

  def broadcast[T](id: Int, msg: T)(implicit writes: Writes[T]) =
    viewers(id).foreach {case (_, Viewer(_, _, channel)) =>
      push(channel, msg)
    }

  def push[T](channel: Channel[Message], msg: T)(implicit writes: Writes[T]) =
    channel push Json.toJson(msg)
}
