package ui

import views.html.helper.FieldConstructor
import play.api.i18n.Lang

object Helpers {
  implicit def fields(implicit l: Lang) = FieldConstructor(views.html.field.apply)
}
