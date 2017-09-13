package ubodin

import chandu0101.scalajs.react.components._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import chandu0101.scalajs.react.components.ReactSearchBox
import ubodin.UBOdinAppRouter.Page
import org.scalajs.dom.ext.Ajax
import scala.concurrent._
import ExecutionContext.Implicits.global

object UBOdinHomePage {
  import RCustomStyles._

  case class ComponentInfo(name: String, imagePath: String, route: Page, tags: Stream[String])

  val deviceFingerprint = fingerprint.Fingerprint.get();
  var userCleaningJobs:Option[Vector[CleaningJob]] = None
  var allCleaningJobs:Option[Vector[CleaningJob]] = None
  var loggedInUserInfo:Option[UserInfoData] = None
  
  object Style {

    val info = Seq(
      MsFlexAlign := "center" ,
      MsFlexDirection := "column" ,
      WebkitAlignItems := "center" ,
      WebkitBoxAlign := "center" ,
      WebkitBoxDirection := "normal" ,
      WebkitBoxOrient := "vertical" ,
      WebkitFlexDirection := "column" ,
      ^.alignItems := "center" ,
      ^.backgroundColor := "#eeeeee" ,
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.flexDirection := "column" ,
      ^.fontSize := "18px" ,
      ^.fontWeight := "500" ,
      ^.paddingBottom := "45px" ,
      ^.paddingTop := "85px")

    val infoContent = Seq(^.fontWeight := 500, ^.fontSize := "18px")

    val infoLink = Seq(
      ^.color := "#ff4081" ,
      ^.padding := "0 5px" ,
      ^.textDecoration := "none")

    val searchSection = Seq(
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.margin := "50px" ,
      ^.marginBottom := "15px")

    val componentsGrid = Seq(
      MsFlexWrap := "wrap" ,
      WebkitFlexWrap := "wrap" ,
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.flexWrap := "wrap" ,
      ^.margin := "30px" )

  }
  
  



  case class State(filterText: String = "", results: Vector[ComponentInfo], deviceID:String, userInfo:Option[UserInfoData], cleaningJobs: Option[Vector[CleaningJob]] )

  class Backend(t: BackendScope[RouterCtl[Page], State]) {

    def requestCleaningJobList(deviceID:String) : Callback =  {
      val url = "/ajax/LoginRequest"
      val data = upickle.write(LoginRequest(deviceID))
      val req = upickle.write(AjaxRequestWrapper(deviceID, "LoginRequest", data))
      Callback.future(Ajax.post(url, req).map { xhr =>
        updateStateFromAjaxCall(xhr.responseText, t) 
      })
    }

    def updateStateFromAjaxCall(responseText: String, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      //val curQuizItem = scope.state.currentQuizItem
      println("updateStateFromAjaxCall: " + responseText)
      upickle.read[LoginResponse](responseText) match {
        case LoginResponse(UserInfoResponse(userInfoData), CleaningJobListResponse(cleaningJobs)) => {
          allCleaningJobs = Some(cleaningJobs)
          userCleaningJobs = Some(Vector[CleaningJob]().union(userInfoData.userSchedule.scheduleItems.map(uSchItm => uSchItm.cleaningJob)))
          loggedInUserInfo = Some(userInfoData)
          UBOdinAppRouter.homePageMenu = Vector[ComponentInfo]().union(userCleaningJobs.get.map( cleaningJob => ComponentInfo(cleaningJob.name, cleaningJob.imagePath, UBOdinAppRouter.CleaningJobPage(cleaningJob.cleaningJobID), cleaningJob.tags)) )
          println(UBOdinAppRouter.homePageMenu)
          t.modState(_.copy(results = UBOdinAppRouter.homePageMenu, filterText = "", deviceID = deviceFingerprint, userInfo = loggedInUserInfo, cleaningJobs = userCleaningJobs ))
        }
        case _ => Callback.info("unknown ajax response: doing nothing.")
      }
    }
    
    def onTextChange(text: String) = {
     val results = UBOdinAppRouter.homePageMenu.filter(c => c.tags.exists(s => s.contains(text)))
      t.modState(_.copy(results = results, filterText = text, deviceID = deviceFingerprint, userInfo = loggedInUserInfo, cleaningJobs = userCleaningJobs ))
    }
    
    def start : Callback  = {
      requestCleaningJobList(deviceFingerprint)
    }
    
    def render(P: RouterCtl[Page], S: State) = {
      val userName = S.userInfo match {
        case None => "User"
        case Some(UserInfoData(_, fname, _, _, _)) => fname
      }
      <.div(
        <.div(Style.info, ^.key := "info")(
          <.h3(Style.infoContent)(s"Hello $userName.  Please Select your Cleaning Job."),
          LocalDemoButton(name =s"Welcome $userName",linkButton =  true,href  = "#/account?fp="+deviceFingerprint)
        ),
        <.div(Style.searchSection)(
         ReactSearchBox(onTextChange = onTextChange),
          !S.filterText.isEmpty ?= <.strong(^.alignSelf := "center" ,^.paddingLeft := "30px")(s"Results: ${S.results.length}")
        ),
        <.div(Style.componentsGrid)(
            S.results.map(c => ComponentGridItem(c.name, c.route, c.imagePath,P))
        )
      )
    }
  }

  val component = ReactComponentB[RouterCtl[Page]]("homepage")
    .initialState(State("", Vector[ComponentInfo](), deviceFingerprint, None, None))
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .build
    

  def apply(ctrl: RouterCtl[Page]) = component(ctrl)

}
