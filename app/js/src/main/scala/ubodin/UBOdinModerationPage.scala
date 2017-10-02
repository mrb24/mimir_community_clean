package ubodin

import chandu0101.scalajs.react.components._
import ubodin.fascades.{LatLng, Marker, Icon, Size, Point}
import ubodin.MyMuiSnackbar
import chandu0101.scalajs.react.components.materialui._
import ubodin.ODInTable.Config
//import chandu0101.scalajs.react.components.materialui.{ Vertical, Horizontal, Origin, MuiPopover, MuiRaisedButton, MuiFlatButton, MuiDialog}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import org.scalajs.dom.raw.{Position, PositionError, NavigatorGeolocation}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.util.{Failure, Success}
import ubodin.UBOdinAppRouter.Page
import org.scalajs.dom.ext.Ajax
import jdk.nashorn.internal.runtime.Undefined
import ubodin.ODInTable.Model
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import org.scalajs.dom.{WebSocket, MessageEvent, Event, CloseEvent, ErrorEvent}
import scala.util.{Success, Failure}

object UBOdinModerationPage {
  import RCustomStyles._
  import Mui.SvgIcons.{ ActionInfo, AlertWarning, ActionInfoOutline, AlertError, AlertErrorOutline, ActionCheckCircle }
  import MuiAutoCompleteFilters.caseInsensitiveFilter
  
  val deviceFingerprint = fingerprint.Fingerprint.get();
  var cleaningJobID = "0"
  
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
      ^.paddingBottom := "1px" ,
      ^.paddingTop := "65px")

    val infoContent = Seq(^.fontWeight := 500, ^.fontSize := "18px")

    val infoLink = Seq(
      ^.color := "#ff4081" ,
      ^.padding := "0 5px" ,
      ^.textDecoration := "none")
      
    val taskSection = Seq(
      MsFlexWrap := "wrap" ,
      WebkitFlexWrap := "wrap" ,
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.flexWrap := "wrap" ,
      ^.margin := "30px",
      ^.float := "left",
      ^.minWidth := "264px",
      ^.height := "84%",
      ^.width := "100%")
      
    val cleaningSection = Seq(
      ^.display := "block" ,
      ^.margin := "30px",
      ^.minHeight := "84%" )
      
    val cleaningTableSection = Seq(
      ^.display := "block" ,
      ^.margin := "30px",
      ^.minHeight := "388px" )
  }
  
  case class State(cleaningJobType:String = "MOD",
                   progressState:ProgressState = ProgressState(0),
                   dialogState:DialogState = DialogState(false,"","", None, None),
                   popoverState:PopoverState = PopoverState(false,"","",None),
                   snackState:SnackState = SnackState(false,"",""),
                   taskTreeState:TaskTreeState = TaskTreeState(false, None),
                   taskListState:TaskListState = TaskListState(false, List()),
                   selectedTask:Option[CleaningJobTaskGroup] = None,
                   drawerState:DrawerState = DrawerState(Seq(), Seq(), None, false, true, false),
                   baseTheme: MuiRawTheme = Mui.Styles.LightRawTheme,
                   backgroundColor: js.UndefOr[MuiColor] = js.undefined,
                   webSocketState: WebSocketState = WebSocketState(0, None, scala.collection.mutable.Map())
                   ){
  val theme: MuiTheme =
    Mui.Styles.getMuiTheme(backgroundColor.fold(baseTheme)(
      color ⇒ baseTheme.copy(palette = baseTheme.palette.copy(canvasColor = color)))
    )
  def wslog(msg:String) = {
    Callback.info(msg).runNow()
    copy()
  }
}

  case class DialogState(open:Boolean, title:String, message:String, content:Option[ReactElement], actions:Option[ReactNode])
  case class PopoverState(open:Boolean, title:String, message:String, content:Option[ReactElement])
  case class SnackState(open:Boolean, title:String, message:String)
  case class TaskTreeState(loaded:Boolean, data:Option[TreeItem])
  case class TaskListState(loaded:Boolean, data:List[CleaningJobTaskGroup])
  case class DrawerState(choices:Seq[MainMenuChoice], selected:Seq[String], selectedWaitingCallback:Option[MainMenuChoice] = None, open:Boolean, docked:Boolean, right:Boolean)
  case class ProgressState(loading:Int)
  case class WebSocketState(outMessageCount:Int, ws:Option[WebSocket], handlers:scala.collection.mutable.Map[Int, String => Callback])
  
  case class MainMenuChoice(id: String, text: String, settingOption:Option[SettingOption])
    
  case class Backend(t : BackendScope[RouterCtl[Page],State]) {
    
    var watchID : Int = 0
    
    val ref = Ref[TopNode]("theRef")
    
    
    
    ///------------------------------------
    /// WebSocket Code
    ///------------------------------------
    val urlRegex = "([https]+):\\/\\/([\\d\\w.-]+)(?:(:[\\d]+)).*".r
    val (wsscheme, wshost, wsport) = dom.document.URL match {
      case urlRegex(scheme, host) => (if(scheme.equals("https")) "wss" else "ws" , host, "")
      case urlRegex(scheme, host, port) => (if(scheme.equals("https")) "wss" else "ws" , host, port)
      case _ => ("wss", "localhost", ":8089")
    }
    val wsurl = s"$wsscheme://${wshost}$wsport/ws/"
    def wsRequest(ws: WebSocket, requestType:String, msg: String, cbo:Option[String => Callback] = None ): Callback = {
       t.modState( s => s.copy(progressState = ProgressState(s.progressState.loading +1 ), webSocketState = s.webSocketState.copy(outMessageCount = s.webSocketState.outMessageCount + 1, handlers = cbo match { 
           case None => s.webSocketState.handlers
           case Some(cb) => s.webSocketState.handlers += (s.webSocketState.outMessageCount + 1) -> cb
         } )).wslog(s"sending web socket request: $requestType") , t.state.flatMap( s => {
            val req = upickle.write(WSRequestWrapper(deviceFingerprint, requestType, s.webSocketState.outMessageCount, msg)) 
            Callback(ws.send(req))
          }))
    } 
    
      def createWebSocket(successCB:Option[() => Callback] = None, failureCB:Option[ () => Callback], openCB:Option[() => Callback] = None): Callback = {
    
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
            direct.modState(_.copy().wslog(s"WebSocket Message: UID: ${respWrapped.responseUID} Type: ${respWrapped.responseType} Size: ${respWrapped.response.length()}"),
            (direct.state.webSocketState.handlers.getOrElse(respWrapped.responseUID, {
              val resp = UBOdinAjaxResponse.readResponse(respWrapped.responseType, respWrapped.response)
              resp match {
                case response:NoResponse => defaultHandler( Callback.info("WS NoResponse"))
                case response:CleaningJobTaskListResponse => defaultHandler(cleaningJobTaskListResponse(response, t))
                case response:LoadCleaningJobResponse => defaultHandler( 
                  t.modState(s => s.copy(cleaningJobType = response.job.jobType)))
                case x => defaultHandler( Callback.warn("WS Response Not Handled: " + x.toString()) )
              }
            })(respWrapped.response)) >> t.modState(s=>s.copy(progressState = ProgressState(s.progressState.loading -1), webSocketState = s.webSocketState.copy(handlers = (s.webSocketState.handlers -= respWrapped.responseUID)))))
          }
    
          def onerror(e: ErrorEvent): Unit = {
            // Display error message
            direct.modState(_.wslog(s"Error: ${e}"))
          }
    
          def onclose(e: CloseEvent): Unit = {
            // Close the connection
            direct.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = None)).wslog(s"Closed: ${e.reason}"))
          }
    
          // Create WebSocket and setup listeners
          val ws = new WebSocket(wsurl)
          ws.onopen = onopen _
          ws.onclose = onclose _
          ws.onmessage = onmessage _
          ws.onerror = onerror _
          
          def onopen(e: Event): Unit = {
            wsRequest(ws, "NoRequest", upickle.write(NoRequest())).runNow()
            openCB match {
              case Some(cb) => direct.modState(_.wslog("Connected."), cb())
              case None => direct.modState(_.wslog("Connected."))
            }
          }
          
          ws
        }
    
        // Here use attemptTry to catch any exceptions in connect.
        connect.attemptTry.flatMap {
          case Success(ws)    => successCB match {
            case Some(cb) => t.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = Some(ws))), cb())
            case None => t.modState(s=>s.copy(webSocketState = s.webSocketState.copy(ws = Some(ws))))
          }
          case Failure(error) => failureCB match {
            case Some(cb) => cb()
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
    
    
    
    def requestLoadCleaningJobsModerationList() : Callback = {
      val url = "/ajax/LoadCleaningJobsModerationRequest"
      val data = upickle.write(LoadCleaningJobsModerationRequest(deviceFingerprint))
      ajaxRequest(url, data, responseText => {
        val cleaningJobLoadResponse = upickle.read[CleaningJobsModerationResponse](responseText)
        val newListData = cleaningJobLoadResponse.cleaningJobsTasks.map{
          case (cleaningJobID, moderationTasks:Vector[CleaningJobTaskGroup]) => {
            moderationTasks
          }
        }.flatten.toList
        t.modState(s => s.copy(taskListState = TaskListState(true, newListData)))
      })
    }
    
    def requestCleaningJobTaskList(cleaningJobID:String, count:Int, offset:Int) : Callback = {
      val url = "/ajax/CleaningJobTaskListRequest"
      val data = upickle.write(CleaningJobTaskListRequest(deviceFingerprint, cleaningJobID, count, offset))
      ajaxRequest(url, data, responseText => {
        val cleaningJobTaskListRespons = upickle.read[CleaningJobTaskListResponse](responseText)
        cleaningJobTaskListResponse(cleaningJobTaskListRespons, t)
        //cleaningJobTaskTreeResponse(xhr.responseText, t) 
      })
    }
    
    def requestCleaningJobTaskFocusedList(cleaningJobID:String, rowIDs:Seq[String], cols:Seq[String] = Seq()) : Callback = {
      val url = "/ajax/CleaningJobTaskFocusedListRequest"
      val data = upickle.write(CleaningJobTaskFocusedListRequest(deviceFingerprint,cleaningJobID, rowIDs, cols))
      ajaxRequest(url, data, responseText => {
        val cleaningJobTaskListRespons = upickle.read[CleaningJobTaskListResponse](responseText)
        cleaningJobTaskListResponse(cleaningJobTaskListRespons, t)
        //cleaningJobTaskTreeResponse(xhr.responseText, t) 
      })
    }
    

    def cleaningJobTaskTreeResponse(cleaningJobTaskListResponse: CleaningJobTaskListResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      println("cleaningJobTaskTreeResponse: " )
      val newTreeData = cleaningJobTaskListResponse.cleaningJobTasks.map(cleaningJobTask => {
        TreeItem(toElement(cleaningJobTask))
      }).toSeq 
      val treeRoot = TreeItem("root", newTreeData : _*)
      t.modState(s => s.copy(taskTreeState = TaskTreeState(true, Some(treeRoot ))))
    }
    
    def cleaningJobTaskListResponse(cleaningJobTaskListResponse: CleaningJobTaskListResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      println("cleaningJobTaskListResponse: " )
      val newListData = cleaningJobTaskListResponse.cleaningJobTasks.toList
      t.modState(s => s.copy(taskListState = TaskListState(true, newListData)))
    }
    
    def requestRepair(cleaningJobID:String, reason:CleaningJobTask, repairValue:String) : Callback = {
      val url = "/ajax/CleaningJobModRepairRequest"
      val data = upickle.write(CleaningJobModRepairRequest(deviceFingerprint, cleaningJobID, reason.source, reason.varid.toInt, reason.args, repairValue))
      ajaxRequest(url, data, responseText => {
        val cleaningJobRepairRespons = upickle.read[NoResponse](responseText)
        t.state.flatMap(s => requestLoadCleaningJobsModerationList())
      })
      
    }
    
    
    ///----------------------------------------
    ///--------Task List code
    ///----------------------------------------
    def onListItemSelect(item: String) : Callback = {
      val content = s"Selected Item: $item <br>"
      Callback.alert(s"Value in list selected. ${content}")
    }
    
    ///----------------------------------------
    /// MUI Task List Code
    ///----------------------------------------
    val muiTaskListTouchEvent: ReactTouchEvent => Callback = 
       e => Callback.info("muiTaskListTouchEvent")
       
    val muiTaskListTouchTapEvent: CleaningJobTaskGroup => TouchTapEvent => Callback = 
       reasons => e => t.modState(s => s.copy(selectedTask = Some(reasons)), openModRepairDialog())
       
    val muiTaskListAnyEvent: scala.scalajs.js.Any => Callback = 
       e => Callback.info("muiTaskListAnyEvent")  
       
    val muiTaskListFocusEvent: (ReactFocusEvent, Boolean) => Callback = 
       (e, b) => Callback.info("muiTaskListFocusEvent")
       
    val muiTaskListEvent: ReactEvent => Callback = 
       e => Callback.info("muiTaskListEvent")
    
    def toElement(tasks:CleaningJobTaskGroup) : ReactElement = {
      tasks.tasks.map(element => s"${element.english}").distinct.foldLeft(None:Option[ReactElement])( (init, element) => init match {
        case None => Some(<.div(s"$element")) 
        case Some(initElement) => Some(<.div(initElement,<.br(),element))
      }).get
    }
       
    ///----------------------------------------
    ///--------Task Tree code
    ///----------------------------------------
    def onTreeItemSelect(item: String, parent: String, depth: Int): Callback = {
      val content = s"Selected Item: $item <br>"
      Callback.alert(s"Value in list selected. ${content}")
    }
    
    ///----------------------------------------
    /// Main Menu Drawer Code
    ///----------------------------------------
    val toggleDrawerOpenCb: Callback =
        t.modState(s => s.copy(drawerState = s.drawerState.copy(open = !s.drawerState.open)))
    
    val toggleDrawerOpen: (ReactEventI, Boolean) => Callback =
      (e, b) => toggleDrawerOpenCb
 
    val mainMenuFromHeaderClick: TouchTapEvent => Callback = 
       e => toggleDrawerOpenCb
  
    val onDrawerRequestChange: (Boolean, String) => Callback =
      (open, reason) =>
        Callback.info(s"onRequestChange: open: $open, reason: $reason") >>
        toggleDrawerOpenCb
  
    val selectDrawerItem: MainMenuChoice => TouchTapEvent => Callback = 
      choice => e => choice.settingOption match {
        case _ =>  t.state.flatMap(s => {
          if(!s.drawerState.selected.contains(choice.id)){
            t.modState(s => s.copy(drawerState = s.drawerState.copy( selected = s.drawerState.selected.union(Seq(choice.id)).distinct)))
          }
          else{
            t.modState(s => s.copy(drawerState = s.drawerState.copy( selected = s.drawerState.selected.filter(!_.equals(choice.id)))))
          }
        })
      }
    
    UBOdinAppHeader.mainMenuClick = Some(mainMenuFromHeaderClick)
    
    ///----------------------------------------
    ///-----Popover code
    ///----------------------------------------
    def togglePopover(title:String, message:String, content:Option[ReactElement] = None): Callback =
      t.modState(s => s.copy(popoverState = s.popoverState.copy(open = !s.popoverState.open, title = title, message = message, content = content)))
      
    val popoverRequestClose: String => Callback = {
      key => t.modState(s => s.copy(popoverState = s.popoverState.copy(open = false)))
    }
    
    val popoverClose: TouchTapEvent => Callback = e => popoverRequestClose("")
    
      
    ///----------------------------------------
    ///-----Dialog code
    ///----------------------------------------
    val defaultDialogActions: ReactNode = js.Array(
        MuiFlatButton(key = "1", label = "Cancel", secondary = true, onTouchTap = handleDialogCancel)(),
        MuiFlatButton(key = "2", label = "Reject", secondary = true, onTouchTap = handleDialogRepair)(),
        MuiFlatButton(key = "3", label = "Accept", secondary = true, onTouchTap = handleDialogAcknowledge)()
      )  
      
    def handleDialogCancel: TouchTapEvent => Callback =
    e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> togglePopover("","Canceled")//Callback.info("Cancel Clicked")

    def handleDialogRepair: TouchTapEvent => Callback =
      e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> showSnack("Rejected", "The user contribution has been rejected")//Callback.info("Submit Clicked")
  
    def handleDialogAcknowledge: TouchTapEvent => Callback =
      e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> showSnack("Accept", "The user contribution has been accepted")//Callback.info("Submit Clicked")
      
    def openDialog(title:String, message:String): Callback =
      t.modState(s => s.copy(dialogState = DialogState(true, title, message, None, None))) >> Callback.info("Opened")
   
    def handleDialogModRepair: (CleaningJobTask, String) => TouchTapEvent => Callback =
      (reason, repairValue) => e => requestRepair(cleaningJobID, reason, repairValue) >> t.modState(s => s.copy(dialogState = DialogState(!s.dialogState.open, s.dialogState.title, s.dialogState.message, None, None))) >> showSnack("Repaired", "The data has been repaired")//Callback.info("Submit Clicked")  
      
    def openModRepairDialog(): Callback = {
      t.state.flatMap( s =>
        {
          val selectedTask = s.selectedTask
          println(selectedTask)
          selectedTask match { 
            case Some(taskGroup) => {
              val outerDialogActions: ReactNode = js.Array(MuiFlatButton(key = "1", label = "Cancel", secondary = true, onTouchTap = handleDialogCancel)()) 
              val content = <.div(
                  <.h3("Moderation Repair Dialog"),
                  taskGroup.tasks.map(task => { 
                    <.div()(
                      <.div(^.display := "inline-block", ^.margin := "40px", ^.maxWidth := "40%", ^.verticalAlign := "top")(
                          <.h4("User Contributed Repair: "),
                          <.div( s"${task.english}" )   
                      ),
                      <.div(^.display := "inline-block", ^.margin := "40px", ^.maxWidth := "40%", ^.verticalAlign := "top")( 
                            MuiFlatButton(key = "1", label = "Reject", secondary = true, onTouchTap = handleDialogCancel)(),
                            MuiFlatButton(key = "2", label = "Accept", secondary = true, onTouchTap = handleDialogModRepair(task, upickle.read[CleaningJobTaskModerationRepair](task.repair.repairJson).value))()
                      )
                   )
                })
              )
              t.modState(s => s.copy( dialogState = DialogState(true, "", "", Some(content), Some(outerDialogActions)))) >> Callback.info("Moderation Repair Dialog Opened")
            }
            case None => Callback.info("No Task Selected not opening moderation dialog")
          }
        }) 
    }
      
    ///----------------------------------------
    ///--------SnackBar code
    ///----------------------------------------
    val snackUndo: ReactEvent => Callback =
      e =>  t.modState(s => s.copy(snackState = s.snackState.copy(open = !s.snackState.open))) >> Callback(dom.window.alert("We reverted the repair"))
  
    val snackCloseRequested: String => Callback =
      reason => t.modState(s => s.copy(snackState = s.snackState.copy(open = !s.snackState.open))) >> Callback.info(s"onRequestClose: $reason")
  
    val toggleSnack: ReactEventH => Callback =
      e => t.modState(s => s.copy(snackState = s.snackState.copy(open = !s.snackState.open)))
      
    def showSnack(title: String, message:String) : Callback = {
        t.modState(s => s.copy(snackState = SnackState(true, title, message)))
     }
    
      
    ///---------------------------------------
    ///  Theme Code
    ///---------------------------------------
    val onThemeChanged: (ReactEvent, Int, MuiRawTheme) ⇒ Callback =
      (e, idx, theme) ⇒ t.modState(s => s.copy(baseTheme = theme))
  
    //boot up and load initial data
    def start : Callback  = {
      createWebSocket( None, Some( requestLoadCleaningJobsModerationList _), Some( requestLoadCleaningJobsModerationList _) )
      //requestGetCleaningJobSettingsOptions(cleaningJobID) >> requestGetCleaningJobSettings(cleaningJobID) >> requestCleaningJobTaskList(cleaningJobID, 10, 0) >> requestCleaningData(cleaningJobID)
    }
      
    //render  
    def render(P: RouterCtl[Page], S: State) = {
       println(s"render: ${ S.drawerState.selected } ")
      val dialogTitle: ReactNode = js.Array(S.dialogState.title)
      
      val snackAction: ReactNode = js.Array("undo")
      
      MuiMuiThemeProvider(muiTheme = S.theme)(
      <.div( 
        MuiPaper()(
        <.div(Style.info, ^.key := "info")(
          /*<.h3(Style.infoContent)("You Have Been Checked In to Your Job Site"),
          LocalDemoButton(name ="I Am Leaving Job",linkButton =  true,href  = "#/leavingjob?fp="+deviceFingerprint)*/ 
          if(S.progressState.loading > 0)
            MuiLinearProgress( mode = DeterminateIndeterminate.indeterminate)()
          else 
            <.div(^.height := "4px")()
        ),
        <.div(Style.taskSection)(
           MuiDrawer(
            onRequestChange = onDrawerRequestChange,
            openSecondary   = S.drawerState.right,
            open            = S.drawerState.open,
            docked          = S.drawerState.docked)(
            /* hack in a cheesy centered avatar */
            MuiAvatar(
              key             = "avatar",
              size            = 112,
              backgroundColor = Mui.Styles.colors.red400,
              style           = js.Dynamic.literal(
                margin  = "auto",
                display = "block",
                padding = "10px"
              ))("@:)":ReactNode),
            MuiDropDownMenu[MuiRawTheme](key = "themeDropdown", value = S.baseTheme, onChange = onThemeChanged)(
              MuiMenuItem[MuiRawTheme](key = "LightRawTheme", primaryText = "LightRawTheme":ReactNode, value = Mui.Styles.LightRawTheme)(),
              MuiMenuItem[MuiRawTheme](key = "DarkRawTheme",  primaryText = "DarkRawTheme":ReactNode,  value = Mui.Styles.DarkRawTheme )()
            ),
            S.drawerState.choices map (c =>
              MuiMenuItem(
                key         = c.id,
                primaryText = c.text: ReactNode,
                checked     = S.drawerState.selected.contains(c.id),
                onTouchTap  = selectDrawerItem(c)
              )()
            )
          ),
          MuiPaper()(
            MuiList(key = "task_list")(
                S.taskListState.data.zipWithIndex.map(listElement => {
                  MuiListItem(
                      key = s"item${listElement._2}", 
                      primaryText = toElement(listElement._1),
                      leftIcon           = {if(S.selectedTask.equals(listElement._1))ActionCheckCircle()() else AlertWarning()()},
                      //rightIcon          = AlertWarning()(),
                      //onKeyboardFocus    = muiTaskListFocusEvent,
                      //onMouseLeave       = muiTaskListEvent,
                      //onMouseEnter       = muiTaskListEvent,
                      //onNestedListToggle = muiTaskListAnyEvent,
                      //onTouchStart       = muiTaskListTouchEvent,
                      onTouchTap         = muiTaskListTouchTapEvent(listElement._1))()
                }) : _*
            )
          )
          /*ReactListView(
            items = S.taskListState.data,
            showSearchBox = true,
            onItemSelect = onListItemSelect _
          )*/
          /*ReactTreeView(
            root = S.taskTreeState.data match { 
              case None => {
                println("wtf--------")
                TreeItem("loading...")
              }
              case Some(data) => {
                println(data)
                data
              }
            },
            openByDefault = true,
            onItemSelect = onTreeItemSelect _,
            showSearchBox = true
          )  */
        ),
        
        <.div(),
          MuiPopover(
            open = S.popoverState.open,
            anchorEl = ref(t),
            anchorOrigin = Origin(Vertical.bottom, Horizontal.left),
            targetOrigin = Origin(Vertical.top,    Horizontal.left),
            onRequestClose = popoverRequestClose  
          )(
              S.popoverState.content match {
                case None => {
                  <.div(
                    ^.padding := "20px",
                    <.h2(S.popoverState.title),
                    <.p(S.popoverState.message),
                    MuiRaisedButton(
                      primary = true,
                      label = "Dismiss",
                      onTouchTap = (e: ReactEvent) => togglePopover("","")
                    )())
                }
                case Some(content) => content
              }
          ),
          MuiDialog(
            title = dialogTitle,
            actions = S.dialogState.actions match {
              case None => defaultDialogActions
              case Some(stateActions) => stateActions 
            },
            open = S.dialogState.open/*,
            onRequestClose = CallbackDebug.f1("onRequestClose")*/
          )(
            <.div(^.ref := ref)(
              S.dialogState.content match {
                case None => S.dialogState.message
                case Some(content) => content
              }
            )
          ),
          MyMuiSnackbar(
            autoHideDuration = 5000,
            message          = S.snackState.message,
            action           = snackAction,
            onActionTouchTap = snackUndo,
            onRequestClose   = snackCloseRequested,
            open             = S.snackState.open
          )()
        )
      ))
     }
  }
  
  val component = ReactComponentB[RouterCtl[Page]]("ModerationPage")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .build

  
    
  def apply(ctrl: RouterCtl[Page]) = {
    println("UBOdinModerationPage Apply: " )
    component(ctrl)
  }
 

}