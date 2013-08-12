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

  case class Join(id: Int)
  case class Leave(id: Int, viewerId: String)
  case class WelcomeViewer(id: Int, viewerId: String, channel: Channel[Message])
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

  var viewers : Map[Int, Map[String, Channel[Message]]] =
    Map.empty.withDefault(_ => Map.empty)

  def receive = {
    case Join(id) =>
      val viewerId = UUID.randomUUID.toString
      val enumerator = Concurrent.unicast[Message](
        self ! WelcomeViewer(id, viewerId, _)
      )

      sender ! (createIteratee(id, viewerId), enumerator)
    case WelcomeViewer(id, viewerId, channel) =>
      viewerCount = viewerCount.updated(id, viewerCount(id) + 1)
      viewers = viewers.updated(id, viewers(id) + (viewerId -> channel))

      broadcast(id, ViewerUpdate(viewerCount(id)))
      push(viewers(id)(viewerId), CommentList(bookDao.get(id).get.comments))
    case ErrorHappen(id, viewerId, message) =>
      push(viewers(id)(viewerId), Error(message))
    case AddComment(id, comment) =>
      bookDao addComment (id, comment)
      broadcast(id, CommentAdded(comment))
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
    viewers(id).foreach {case (_, channel) =>
      push(channel, msg)
    }

  def push[T](channel: Channel[Message], msg: T)(implicit writes: Writes[T]) =
    channel push Json.toJson(msg)
}
