package ubodin

import chandu0101.scalajs.react.components.RCustomStyles
import chandu0101.scalajs.react.components.RCustomStyles._
import chandu0101.scalajs.react.components.materialui.Mui
import chandu0101.scalajs.react.components.materialui.MuiIconButton
import chandu0101.scalajs.react.components.materialui.TouchTapEvent
import chandu0101.scalajs.react.components.materialui.MuiMuiThemeProvider
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object UBOdinAppHeader {
  import Mui.SvgIcons.{ NavigationMenu }
  
  object Style {

    val headerStyle: Seq[TagMod] = Seq(
      ^.background := "#4286f4",
      ^.fontSize := "1.5em",
      ^.padding := "0px",
      ^.margin := "0px",
      ^.position := "fixed",
      ^.width := "100%",
      ^.zIndex := "5"
    )

    val menuNav = Seq(
      MsFlexAlign := "center" ,
      WebkitAlignItems := "center" ,
      WebkitBoxAlign := "center" ,
      ^.alignItems := "center" ,
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.height := "64px" ,
      ^.lineHeight := "64px" ,
      ^.margin := "0 3rem"
    )

    val logo = Seq(
      ^.color := "rgb(244, 233, 233)",
      ^.textDecoration := "none",
      ^.width := "150px"
    )

    val menuItem = Seq(
      ^.padding := "20px",
      ^.color := "rgb(244, 233, 233)",
      ^.textDecoration := "none")

    val menuItemHover = Seq(^.background := "#f1453e")

  }
  
  var mainMenuClick: Option[(TouchTapEvent) => Callback] = None

  case class State(menuHover: String = "")

  class Backend(t: BackendScope[_, State]) {

    def onMouseEnter(menu: String) = t.modState(_.copy(menuHover = menu))

    val mainMenuNoneClick: TouchTapEvent => Callback = 
       e => Callback.info("No Main Menu OnTouch")
    
    val onMouseLeave = t.modState(_.copy(menuHover = ""))

    def render(S: State) = {
      val github: String = "Mimir"
      <.header(Style.headerStyle)(
        <.nav(Style.menuNav)(
          <.a(Style.logo, ^.href := "#")(<.img(^.src := "app/images/mimir.png", ^.width := "150px")),
          <.div(^.marginLeft := "auto")(
            MuiMuiThemeProvider()(
              MuiIconButton(onTouchTap = mainMenuClick.getOrElse(mainMenuNoneClick).asInstanceOf[(TouchTapEvent) => Callback])(NavigationMenu()())
            ),
            <.a(
              ^.target :="_blank" ,
              (S.menuHover == github) ?= Style.menuItemHover,
              Style.menuItem,
              ^.href := "https://github.com/UBOdin/mimir/tree/MimirGProMIntegration",
              ^.onMouseEnter --> onMouseEnter(github),
              ^.onMouseLeave --> onMouseLeave)(github)
          )
        )
      )
    }
  }

  val component = ReactComponentB[Unit]("UBOdinAppHeader")
    .initialState(State())
    .renderBackend[Backend]
    .build

  def apply() = component()

}
