package ubodin

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }



object UBOdinAppRouter {

  trait Page
  
  case object Home extends Page
  case class CleaningJobPage(cleaningJobID:Int) extends Page

  
  val config = RouterConfigDsl[Page].buildConfig { dsl =>
    import dsl._
    
   (trimSlashes
      | staticRoute(root, Home) ~> renderR(ctrl => UBOdinHomePage(ctrl))
      | dynamicRouteCT("job" ~ ("/" ~ int ).caseClass[CleaningJobPage]) ~> dynRenderR((page, ctrl) => UBOdinCleaningJobPage(page, ctrl))
      ).notFound(redirectToPage(Home)(Redirect.Replace))
      .renderWith(layout)
  }

  def layout(c: RouterCtl[Page], r: Resolution[Page]) = {
    println(c)
    println(r.page)
    <.div(
      UBOdinAppHeader(),
      r.render(),
      <.div(^.textAlign := "center", ^.key := "footer")(
        <.hr(),
        <.p("Built by ODIn Lab using scalajs")))
  }

  var homePageMenu = Vector[UBOdinHomePage.ComponentInfo]()
  
  /*val homePageMenu = Vector(
    UBOdinHomePage.ComponentInfo(
      name = "Material UI",
      imagePath = g.materialuiImage.toString,
      route = UBOdinMuiPages(UBOdinMuiRouteModule.AppBar),
      tags = Stream("materialui", "material", "framework")
    )
  )*/

  val baseUrl =
    if (dom.window.location.hostname == "localhost")
      BaseUrl.fromWindowOrigin_/
    else
      BaseUrl.fromWindowOrigin_/ 

  val router = Router(baseUrl, config)

   
}
