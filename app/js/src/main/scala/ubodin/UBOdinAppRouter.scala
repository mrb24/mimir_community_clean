package ubodin

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import scala.scalajs.js.Dynamic.{ global => g }
import org.scalajs.dom.{WebSocket, MessageEvent, Event, CloseEvent, ErrorEvent}
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.ext.Ajax
import scala.util.{Failure, Success}
import scala.scalajs.js.annotation._

object UBOdinAppRouter {

  trait Page
  
  case object Home extends Page
  case object ModerationPage extends Page
  case class CleaningJobPage(cleaningJobID:Int) extends Page
  
  private val cbinfo : String => Callback = msg => Callback.info(msg)
  
  val network = NetworkCommunication(None, Some(("WebSocket Creation Failed!", cbinfo )), None)
  
  val config = RouterConfigDsl[Page].buildConfig { dsl =>
    import dsl._
    
   (trimSlashes
      | staticRoute(root, Home) ~> renderR(ctrl => UBOdinHomePage(ctrl))
      | staticRoute("mod", ModerationPage) ~> renderR(ctrl => UBOdinModerationPage(ctrl))
      | dynamicRouteCT("job" ~ ("/" ~ int ).caseClass[CleaningJobPage]) ~> dynRenderR((page, ctrl) => UBOdinCleaningJobPage(page, ctrl))
      ).notFound(redirectToPage(Home)(Redirect.Replace))
      .renderWith(layout)
  }

  def layout(c: RouterCtl[Page], r: Resolution[Page]) = {
    println(c)
    println(r.page)
    <.div(
      network,
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


case class NetworkCommState(val webSocketState: WebSocketState = WebSocketState(false, 0, None, scala.collection.mutable.Map()),
                 val progressState:ProgressState = ProgressState(0)){
  def wslog(msg:String) = {
    Callback.info(msg).runNow()
    copy()
  }
}  
case class WebSocketState(hasConnected:Boolean, outMessageCount:Int, ws:Option[WebSocket], handlers:scala.collection.mutable.Map[Int, String => Callback])
case class ProgressState(loading:Int)

trait NetworkInterface {
  def network = NetworkCommunication.getBackend
}

object NetworkCommunication {
  val deviceFingerprint = Client.deviceFingerprint;
  private var nbackend:Backend = null
  def getBackend() : Backend = {
    nbackend
  }
  class Backend(t: BackendScope[NetworkCommProps, NetworkCommState]) {
    ///------------------------------------
    /// WebSocket Code
    ///------------------------------------
    val urlRegex = "([https]+):\\/\\/([\\d\\w.-]+)\\/.*".r
    val urlRegexPort = "([https]+):\\/\\/([\\d\\w.-]+)(:[\\d]+).*".r
    val (wsscheme, wshost, wsport) = dom.document.URL match {
      case urlRegexPort(scheme, host, port) => (if(scheme.equals("https")) "wss" else "ws" , host, port)
      case urlRegex(scheme, host) => (if(scheme.equals("https")) "wss" else "ws" , host, "")
      case _ => ("wss", "localhost", ":8089")
    }
    val wsurl = s"$wsscheme://${wshost}$wsport/ws/"
    def wsRequest(ws: WebSocket, requestType:String, msg: String, cbo:Option[String => Callback] = None ): Callback = {
      println("WS Request: " + requestType) 
      t.modState( s => s.copy(progressState = ProgressState(s.progressState.loading +1 ), webSocketState = s.webSocketState.copy(outMessageCount = s.webSocketState.outMessageCount + 1, handlers = cbo match { 
           case None => s.webSocketState.handlers
           case Some(cb) => s.webSocketState.handlers += (s.webSocketState.outMessageCount + 1) -> cb
         } )).wslog(s"sending web socket request: $requestType") , t.state.flatMap( s => {
            println(s"WebSocket handlers ${s.webSocketState.handlers}")
            val req = upickle.write(WSRequestWrapper(deviceFingerprint, requestType, s.webSocketState.outMessageCount, msg)) 
            Callback(ws.send(req))
          }))
    } 
    
      def createWebSocket(successCB:Option[(String, String => Callback)] = None, failureCB:Option[(String, String => Callback)], openCB:Option[(String, String => Callback)] = None): Callback = {
        nbackend = this
        println("Creating WebSocket...")
          
        // This will establish the connection and return the WebSocket
        def connect = CallbackTo[WebSocket] {
    
          // Get direct access so WebSockets API can modify state directly
          // (for access outside of a normal DOM/React callback).
          // This means that calls like .setState will now return Unit instead of Callback.
          val direct = t.accessDirect
          def defaultHandler(cb:Callback) : String => Callback = str => cb
          
          // These are message-receiving events from the WebSocket "thread".
          def onmessage(e: MessageEvent): Unit = {
            val respWrapped = upickle.read[WSResponseWrapper](e.data.toString)
            println(s"WebSocket Message: UID: ${respWrapped.responseUID} Type: ${respWrapped.responseType}")
            direct.modState(_.copy().wslog(s"WebSocket Message: UID: ${respWrapped.responseUID} Type: ${respWrapped.responseType} Size: ${respWrapped.response.length()}"),
            (direct.state.webSocketState.handlers.getOrElse(respWrapped.responseUID, 
                defaultHandler( Callback.warn("WS Response Not Handled: " + respWrapped.responseUID + " -> " + respWrapped.responseType + ": " + e.data.toString) )
              )(respWrapped.response)) >> t.modState(s=>s.copy(progressState = ProgressState(s.progressState.loading -1), webSocketState = s.webSocketState.copy(handlers = (s.webSocketState.handlers -= respWrapped.responseUID)))))
          }
    
          def onerror(e: ErrorEvent): Unit = {
            // Display error message
            direct.modState(_.wslog(s"Error: ${e.toLocaleString()}"))
          }
    
          def onclose(e: CloseEvent): Unit = {
            // Close the connection
            direct.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = None)).wslog(s"Closed: ${e.reason}"), t.state.flatMap(s => s.webSocketState.hasConnected match {
              case true => Callback.info("will fallback to ajax for future requests")
              case false => failureCB match {
                case Some((str,cb)) => cb(str)
                case None => Callback.info("failed to connect: no on failure")
              }
            }))
          }
    
          // Create WebSocket and setup listeners
          println(s"Connecting WebSocket: $wsurl")
          val ws = new WebSocket(wsurl)
          ws.onopen = onopen _
          ws.onclose = onclose _
          ws.onmessage = onmessage _
          ws.onerror = onerror _
          
          def onopen(e: Event): Unit = {
            wsRequest(ws, "NoRequest", upickle.write(NoRequest())).runNow()
            openCB match {
              case Some((str, cb)) => direct.modState(s=>s.copy(webSocketState = s.webSocketState.copy(hasConnected = true)).wslog("Connected."), cb(str))
              case None => direct.modState(s=>s.copy(webSocketState = s.webSocketState.copy(hasConnected = true)).wslog("Connected."))
            }
          }
          
          ws
        }
    
        // Here use attemptTry to catch any exceptions in connect.
        connect.attemptTry.flatMap {
          case Success(ws)    => successCB match {
            case Some((str, cb)) => t.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = Some(ws))), cb(str))
            case None => t.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = Some(ws))))
          }
          case Failure(error) => failureCB match {
            case Some((str,cb)) => cb(str)
            case None => Callback.info(error.toString)
          }
        }
      }
    
      def killWebSocket: Callback = {
        def closeWebSocket = t.state.map(_.webSocketState.ws.foreach(_.close()))
        def clearWebSocket = t.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = None)))
        closeWebSocket >> clearWebSocket
      }
    
    
    ///------------------------------------
    ///  Ajax Loading Code
    ///------------------------------------
    def ajaxRequest(url:String, data:String, cb:String => Callback ): Callback = {
      t.state.flatMap( s => {
        s.webSocketState.ws match {
          //if web socket available, then use that instead
          case Some(webSoc) if(webSoc.readyState == 1) => wsRequest(webSoc, url.split("/").last, data, Some(cb))
          //otherwise, use a stinky old ajax request
          case _ => ajaxRequesta(url, data, cb)
        }
      })
    }
      
    def ajaxRequesta(url:String, data:String, cb:String => Callback ): Callback = {
      val reqType = url.split("/").last
      val req = upickle.write(AjaxRequestWrapper(deviceFingerprint, reqType, data))
      t.modState(s => s.copy(progressState = ProgressState(s.progressState.loading +1)).wslog(s"sending ajax request: $reqType")) >>
      Callback.future(Ajax.post(url, req).map { xhr =>
        cb(xhr.responseText) >> t.modState(s => s.copy(progressState = ProgressState(s.progressState.loading-1)))
      })
    }
    
   
    
    def render(P: NetworkCommProps, S: NetworkCommState) = {
      <.b
    }
    
    def state = t.accessDirect.state
  }
  
  val component = (props:NetworkCommProps) => ReactComponentB[NetworkCommProps]("NetworkCommunication")
    .initialState(NetworkCommState())
    .renderBackend[Backend]
    .componentDidMount(_.backend.createWebSocket(props.successCB, props.failureCB, props.openCB))
    .build
    
  case class NetworkCommProps(
    successCB:Option[(String, String => Callback)], failureCB:Option[(String, String => Callback)], openCB:Option[(String, String => Callback)] 
  )

  def apply(successCB:Option[(String, String => Callback)] = None, failureCB:Option[(String, String => Callback)], openCB:Option[(String, String => Callback)] = None) = {
    val props = NetworkCommProps(successCB, failureCB, openCB)
    component(props)(props)
  }

}

@JSExport
object SendUserActionLog {
  @JSExport
  def send(eventType:String, target:String, xCoord:Int, yCoord:Int, scroll:Int, timestamp:Int, extra:String) :Unit = {
    val url = "/ajax/UserActionLogRequest"
    val data = upickle.write(UserActionLogRequest(NetworkCommunication.deviceFingerprint, eventType, target, xCoord, yCoord, scroll, timestamp, extra))
    val bak = NetworkCommunication.getBackend()
    bak.ajaxRequest(url, data, responseText => {
      val setLogUserAction = upickle.read[NoResponse](responseText)
      Callback.info("User Action Logged to server Server...")
    }).runNow()
  }
}
