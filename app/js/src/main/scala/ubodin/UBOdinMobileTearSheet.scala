package ubodin

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object UBOdinMobileTearSheet {

  object Style extends StyleSheet.Inline {

    import dsl._

    val root = style(
      marginBottom(24 px),
      marginRight(24 px),
      width(360 px)
    )

    val container = style(
      border :=! "solid 1px #d9d9d9",
      height :=! "100%",
      overflow.hidden
    )

    val bottomTear = style(display.block,
      position.relative,
      marginTop :=! "-10px",
      width(360 px)
    )
  }

  case class Backend($: BackendScope[Unit, _]) {
    def render(C: PropsChildren) = {
      <.div(Style.root,
        <.div(Style.container,
          C
        ),
        <.img(Style.bottomTear, ^.src := js.Dynamic.global.bottomTearImage.toString)
      )
    }
  }

  val component = ReactComponentB[Unit]("UBOdinMobileTearSheet")
    .renderBackend[Backend]
    .build

  def apply(children: ReactNode*) = component(children :_*)
}
