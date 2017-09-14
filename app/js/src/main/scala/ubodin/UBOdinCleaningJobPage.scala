package ubodin

import chandu0101.scalajs.react.components._
import ubodin.fascades.{LatLng, Marker, Icon, Size, Point}
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

object UBOdinCleaningJobPage {
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
      
    val mapSection = Seq(
      MsFlexWrap := "wrap" ,
      WebkitFlexWrap := "wrap" ,
      ^.display := "-ms-flexbox" ,
      ^.display := "-webkit-box" ,
      ^.display := "-webkit-flex" ,
      ^.display := "flex" ,
      ^.flexWrap := "wrap" ,
      ^.flexGrow := "1",
      ^.margin := "30px",
      ^.maxHeight := "55%" )
      
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
      ^.height := "84%" )
      
    val cleaningSection = Seq(
      ^.display := "block" ,
      ^.margin := "30px",
      ^.minHeight := "84%" )
      
    val cleaningTableSection = Seq(
      ^.display := "block" ,
      ^.margin := "30px",
      ^.minHeight := "388px" )
  }
  
  case class State(cleaningJobType:String = "",
                   progressState:ProgressState = ProgressState(0),
                   gpsState:GPSLocationState = GPSLocationState(false, LatLng(42.8864 ,-78.8784), None),
                   latLonColsState:LatLonColsState = LatLonColsState("LATITUDE","LONGITUDE",4,5),
                   addrColsState:AddressColsState = AddressColsState("STRNUMBER","STRNAME","CITY","STATE",0,1,3,2),
                   dialogState:DialogState = DialogState(false,"","", None, None),
                   popoverState:PopoverState = PopoverState(false,"","",None),
                   snackState:SnackState = SnackState(false,"",""),
                   taskTreeState:TaskTreeState = TaskTreeState(false, None),
                   taskListState:TaskListState = TaskListState(false, List()),
                   dataTableState:DataTableState = DataTableState(false, CleaningJobData(0,0,Vector(),Vector()), List()),//List(), Vector(), List()),
                   dataTableViewState:DataTableViewState = DataTableViewState(0,Vector()),
                   repairValues:Seq[String] = Seq(),
                   selectedRow:Option[CleaningJobDataRow] = None,
                   selectedCol:Option[String] = None,
                   selectedTask:Option[CleaningJobTaskGroup] = None,
                   drawerState:DrawerState = DrawerState(Seq(), Seq(), None, false, true, false),
                   mapState:MapState = MapState( LatLng(42.8864 ,-78.8784), List() , 14, true),
                   addressState:AddressState = AddressState("","","",""),
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

  case class GPSLocationState(enabled:Boolean, homeLocation: LatLng, location:Option[LatLng])
  case class LatLonColsState(latCol:String, lonCol:String, latColIdx:Int, lonColIdx:Int)
  case class AddressColsState(houseNumber:String, street:String, city:String, state:String, houseNumberIdx:Int, streetIdx:Int, cityIdx:Int, stateIdx:Int)
  case class DialogState(open:Boolean, title:String, message:String, content:Option[ReactElement], actions:Option[ReactNode])
  case class PopoverState(open:Boolean, title:String, message:String, content:Option[ReactElement])
  case class SnackState(open:Boolean, title:String, message:String)
  case class TaskTreeState(loaded:Boolean, data:Option[TreeItem])
  case class TaskListState(loaded:Boolean, data:List[CleaningJobTaskGroup])
  case class DataTableState(loaded:Boolean, data:CleaningJobData, config:List[Config])//columns: List[String], data:Vector[Map[String, Any]], config:List[Config]
  case class DataTableViewState(offset:Int, rows:Vector[ODInTable.Model])
  case class DrawerState(choices:Seq[MainMenuChoice], selected:Seq[String], selectedWaitingCallback:Option[MainMenuChoice] = None, open:Boolean, docked:Boolean, right:Boolean)
  case class MapState(center:LatLng, markers: List[Marker], zoom:Int, cluster:Boolean)  
  case class AddressState(houseNumber:String, streetName:String, city:String, state:String)
  case class ProgressState(loading:Int)
  case class WebSocketState(outMessageCount:Int, ws:Option[WebSocket], handlers:scala.collection.mutable.Map[Int, String => Callback])
  
  case class MainMenuChoice(id: String, text: String, settingOption:Option[SettingOption])
    
  case class Backend(t : BackendScope[RouterCtl[Page],State]) {
    
    var watchID : Int = 0
    
    val ref = Ref[TopNode]("theRef")
    
    ///------------------------------------
    ///  GPS code
    ///------------------------------------
    def handleStartTracking(e : ReactEventH) = {
      watchID =  dom.window.navigator.geolocation.watchPosition(handleGeolocationResponse _,handleGeolocationError _)
    }
    
    def handleStopTracking(e : ReactEventH) = {
      if(watchID > 0) dom.window.navigator.geolocation.clearWatch(watchID)
    }
    
    def handleGeolocationResponse(position : Position) = {
      val lat = position.coords.latitude
      val lng = position.coords.longitude
      println("gps updated: " + lat)
      t.modState(_.copy(gpsState = GPSLocationState(true, LatLng(42.8864 ,-78.8784), Some(LatLng(lat,lng))))) >> requestSetDeviceLocation(lat, lng)
    }
    
    def handleGeolocationError(error : PositionError) = {
      println(s"gps error: ${error.code} : ${error.message}" )
      t.modState(_.copy(gpsState = GPSLocationState(false, LatLng(42.8864 ,-78.8784), None)))
    }
    
    ///------------------------------------
    /// WebSocket Code
    ///------------------------------------
    val wsurl = "wss://localhost:8089/ws/"
    def wsRequest(ws: WebSocket, requestType:String, msg: String, cbo:Option[String => Callback] = None ): Callback = {
       t.modState( s => s.copy(progressState = ProgressState(s.progressState.loading +1 ), webSocketState = s.webSocketState.copy(outMessageCount = s.webSocketState.outMessageCount + 1, handlers = cbo match { 
           case None => s.webSocketState.handlers
           case Some(cb) => s.webSocketState.handlers += (s.webSocketState.outMessageCount + 1) -> cb
         } )).wslog(s"sending web socket request: $requestType") , t.state.flatMap( s => {
            val req = upickle.write(WSRequestWrapper(deviceFingerprint, requestType, s.webSocketState.outMessageCount, msg)) 
            Callback(ws.send(req))
          }))
    } 
    
      def createWebSocket(successCB:Option[(String, String => Callback)] = None, failureCB:Option[(String, String => Callback)], openCB:Option[(String, String => Callback)] = None): Callback = {
    
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
                case response:CleaningJobDataResponse => defaultHandler(cleaningJobDataResponse(response, t))
                case response:GetCleaningJobSettingsResponse => defaultHandler(getCleaningJobSettingsResponse(response, t))
                case response:GetCleaningJobSettingsOptionsResponse => defaultHandler(getCleaningJobSettingsOptionsResponse(response, t))
                case response:LoadCleaningJobResponse => defaultHandler( 
                  t.modState(s => s.copy(cleaningJobType = response.job.jobType),
                  getCleaningJobSettingsOptionsResponse(response.options, t ) >>
                  getCleaningJobSettingsResponse(response.settings, t ) >>
                  cleaningJobDataResponse(response.data, t)))
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
              case Some((str, cb)) => direct.modState(_.wslog("Connected."), cb(str))
              case None => direct.modState(_.wslog("Connected."))
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
          case Some(webSoc) => wsRequest(webSoc, url.split("/").last, data, Some(cb))
          case None => ajaxRequesta(url, data, cb)
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
    
    def requestSetDeviceLocation(lat:Double, lon:Double) : Callback = {
      val url = "/ajax/SetDeviceLocationRequest"
      val data = upickle.write(SetDeviceLocationRequest(deviceFingerprint, lat, lon))
      ajaxRequest(url, data, responseText => {
        val setDeviceLocation = upickle.read[NoResponse](responseText)
        Callback.info("Device Location Set On Server...")
      })
    }
    
    def requestLoadCleaningJob(cleaningJobID:String) : Callback = {
      val url = "/ajax/LoadCleaningJobRequest"
      val data = upickle.write(LoadCleaningJobRequest(deviceFingerprint, cleaningJobID))
      ajaxRequest(url, data, responseText => {
        val cleaningJobLoadResponse = upickle.read[LoadCleaningJobResponse](responseText)
        t.modState(s => s.copy(cleaningJobType = cleaningJobLoadResponse.job.jobType),
        getCleaningJobSettingsOptionsResponse(cleaningJobLoadResponse.options, t ) >>
        getCleaningJobSettingsResponse(cleaningJobLoadResponse.settings, t ) >>
        cleaningJobDataCountResponse(cleaningJobLoadResponse.dataCount, t) >>
        cleaningJobDataResponse(cleaningJobLoadResponse.data, t))
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
      //val curQuizItem = scope.state.currentQuizItem
      println("cleaningJobTaskTreeResponse: " )
      val newTreeData = cleaningJobTaskListResponse.cleaningJobTasks.map(cleaningJobTask => {
        TreeItem(toElement(cleaningJobTask))
      }).toSeq 
      val treeRoot = TreeItem("root", newTreeData : _*)
      t.modState(s => s.copy(taskTreeState = TaskTreeState(true, Some(treeRoot ))))
    }
    
    def cleaningJobTaskListResponse(cleaningJobTaskListResponse: CleaningJobTaskListResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      //val curQuizItem = scope.state.currentQuizItem
      println("cleaningJobTaskListResponse: " )
      val newListData = cleaningJobTaskListResponse.cleaningJobTasks.toList
      t.modState(s => s.copy(taskListState = TaskListState(true, newListData)))
    }
    
    def requestCleaningData(cleaningJobID:String, offset:Option[Int] = None, count:Option[Int] = None) : Callback = {
      val url = "/ajax/CleaningJobDataRequest"
      val data = upickle.write(CleaningJobDataRequest(deviceFingerprint,cleaningJobID, count, offset))
      ajaxRequest(url, data, responseText => {
        val cleaningJobDataRespons = upickle.read[CleaningJobDataResponse](responseText)
        cleaningJobDataResponse(cleaningJobDataRespons, t)
      })
    }
    
    def cleaningJobDataResponse(cleaningJobDataResponse: CleaningJobDataResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      //val curQuizItem = scope.state.currentQuizItem
      println("cleaningJobDataResponse: " )
      /*val newTableData = cleaningJobDataResponse.cleaningJobData.map(cleaningJobData => {
        cleaningJobData.data.map(data => {
          cleaningJobData.cols.zip(data.data).toMap
        })  
      }).head*/
      val newTableData = cleaningJobDataResponse.cleaningJobData.head
      //val cols = newTableData.head.unzip._1.toList
      val config = newTableData.cols.map(col => (col, Some(uncertaintyCellHighlight), None, None) ).toList
      t.modState(s => s.copy(dataTableState = DataTableState(true, newTableData, config), latLonColsState = s.latLonColsState.copy(latColIdx = newTableData.cols.indexOf(s.latLonColsState.latCol), lonColIdx = newTableData.cols.indexOf(s.latLonColsState.lonCol)), addrColsState = s.addrColsState.copy(houseNumberIdx = newTableData.cols.indexOf(s.addrColsState.houseNumber), streetIdx = newTableData.cols.indexOf(s.addrColsState.street), cityIdx = newTableData.cols.indexOf(s.addrColsState.city), stateIdx = newTableData.cols.indexOf(s.addrColsState.state)) )) >> afterNextPage(0, newTableData.data.slice(0,5))
    }
    
    def cleaningJobDataCountResponse(cleaningJobDataCountResponse: CleaningJobDataCountResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      println("cleaningJobDataCountResponse: " )
      val totalDataCount = cleaningJobDataCountResponse.cleaningJobDataCount
      //val config = newTableData.cols.map(col => (col, Some(uncertaintyCellHighlight), None, None) ).toList
      //t.modState(s => s.copy(dataTableState = DataTableState(true, newTableData, config), latLonColsState = s.latLonColsState.copy(latColIdx = newTableData.cols.indexOf(s.latLonColsState.latCol), lonColIdx = newTableData.cols.indexOf(s.latLonColsState.lonCol)), addrColsState = s.addrColsState.copy(houseNumberIdx = newTableData.cols.indexOf(s.addrColsState.houseNumber), streetIdx = newTableData.cols.indexOf(s.addrColsState.street), cityIdx = newTableData.cols.indexOf(s.addrColsState.city), stateIdx = newTableData.cols.indexOf(s.addrColsState.state)) )) >> afterNextPage(0, newTableData.data.slice(0,5))
      Callback.info("count" + totalDataCount)
    }
    
    def requestCreateCleaningJobLocationFilter(cleaningJobID:String, cleaningJobDataID:String, name:String, distance:Double, latCol:String, lonCol:String, lat:Double, lon:Double) : Callback = {
      val url = "/ajax/CreateCleaningJobLocationFilterSettingRequest"
      val data = upickle.write(CreateCleaningJobLocationFilterSettingRequest(deviceFingerprint, cleaningJobDataID, name, distance, latCol, lonCol, lat, lon))
      ajaxRequest(url, data, xhr => {
        t.modState(s => s.copy( drawerState = s.drawerState.copy(selected = s.drawerState.selected.union(Seq(name)).distinct))) >> requestCleaningData(cleaningJobID )
      })
    }
    
    def requestRemoveCleaningJobLocationFilter(cleaningJobID:String, cleaningJobDataID:String, name:String) : Callback = {
      val url = "/ajax/RemoveCleaningJobLocationFilterSettingRequest"
      val data = upickle.write(RemoveCleaningJobLocationFilterSettingRequest(deviceFingerprint, cleaningJobDataID, name))
      ajaxRequest(url, data, xhr => {
        t.modState(s => s.copy( drawerState = s.drawerState.copy( selected = s.drawerState.selected.filter(!_.equals(name))))) >> requestCleaningData(cleaningJobID )
      })
    }
    
    def requestGetCleaningJobSettings(cleaningJobID:String) : Callback = {
      val url = "/ajax/GetCleaningJobSettingsRequest"
      val data = upickle.write(GetCleaningJobSettingsRequest(deviceFingerprint, cleaningJobID))
      ajaxRequest(url, data, responseText => {
        val cleaningJobSettingsRespons = upickle.read[GetCleaningJobSettingsResponse](responseText)
        getCleaningJobSettingsResponse(cleaningJobSettingsRespons, t )
      })
    }
    
    def getCleaningJobSettingsResponse(cleaningJobSettingsResponse: GetCleaningJobSettingsResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      //val curQuizItem = scope.state.currentQuizItem
      println("getCleaningJobSettingsResponse: " )
      cleaningJobSettingsResponse.cleaningJobSettings.map( containedCleaningJobSetting => {
        val cleaningJobSetting = CleaningJobSetting.decontainSetting(containedCleaningJobSetting)
        cleaningJobSetting match {
          case CleaningJobFilterSetting(_,_,_) => Callback.info("Processing CleaninJobFilterSetting...") >> t.modState(s => s.copy( drawerState = s.drawerState.copy( selected = s.drawerState.selected.union(Seq(containedCleaningJobSetting.name)).distinct)))
          case CleaningJobMapClustererSetting() => Callback.info("Processing CleaningJobMapClustererSetting...") >> t.modState(s => s.copy( drawerState = s.drawerState.copy( selected = s.drawerState.selected.union(Seq(containedCleaningJobSetting.name)).distinct)))
          case x => Callback.info(s"We dont need to do anything for $x")
        }
      }).foldLeft(Callback.info("Processing Cleaning Job Settings..."))((init, cb) => init >> cb)
    }
    
    def requestGetCleaningJobSettingsOptions(cleaningJobID:String) : Callback = {
      val url = "/ajax/GetCleaningJobSettingsOptionsRequest"
      val data = upickle.write(GetCleaningJobSettingsOptionsRequest( cleaningJobID))
      ajaxRequest(url, data, responseText => {
        val cleaningJobSettingsOptionsRespons = upickle.read[GetCleaningJobSettingsOptionsResponse](responseText)
        getCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptionsRespons, t )
      })
    }
    
    def getCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptionsResponse: GetCleaningJobSettingsOptionsResponse, scope: BackendScope[RouterCtl[Page], State]): Callback = {
      //val curQuizItem = scope.state.currentQuizItem
      println("getCleaningJobSettingsOptionsResponse: " )
      cleaningJobSettingsOptionsResponse.cleaningJobSettingsOptions.map( containedCleaningJobSettingsOption => {
        val settingOption = SettingOption.decontainOption(containedCleaningJobSettingsOption)
        settingOption match {
          case locationFilterOption@Some(LocationFilterSettingOption(_,_,_)) => Callback.info("Processing LocationFilterSettingOption...") >> t.modState(s => s.copy( drawerState = s.drawerState.copy( choices = s.drawerState.choices.union(Seq(MainMenuChoice(containedCleaningJobSettingsOption.id, containedCleaningJobSettingsOption.name, locationFilterOption))))))
          case mapClustererOption@Some(MapClustererSettingOption()) => Callback.info("Processing MapClustererSettingOption...") >> t.modState(s => s.copy( drawerState = s.drawerState.copy( choices = s.drawerState.choices.union(Seq(MainMenuChoice(containedCleaningJobSettingsOption.id, containedCleaningJobSettingsOption.name, mapClustererOption))))))
          case Some(GISLatLonColsSettingOption(latCol, lonCol)) => Callback.info("Processing GISLatLonColsSettingOption...") >> t.modState(s => s.copy(latLonColsState = s.latLonColsState.copy(latCol = latCol, lonCol = lonCol)))
          case Some(GISAddressColsSettingOption(houseNumber,street,city,state)) => Callback.info("Processing GISAddressColsSettingOption...") >> t.modState(s => s.copy(addrColsState = s.addrColsState.copy(houseNumber = houseNumber, street = street, city = city, state = state)))
          case x => Callback.info(s"We dont need to do anything for $x")
        }
      }).foldLeft(Callback.info("Processing Cleaning Job Settings Options..."))((init, cb) => init >> cb)
    }
    
    def requestRepair(cleaningJobID:String, reasons:CleaningJobTaskGroup, row:ODInTable.Model, repairValues:Seq[String]) : Callback = {
      val url = "/ajax/CleaningJobRepairRequest"
      if(reasons.tasks.length != repairValues.length)
        println("Repairs should match the number of reasons")//throw new Exception("Repairs should match the number of reasons")
      repairValues.zip(reasons.tasks).foldLeft(Callback.info("Repairing..."))((init, reasonRepairValue) => {
        init >> {
          val (repairValue, reason) = reasonRepairValue
          val data = upickle.write(CleaningJobRepairRequest(deviceFingerprint, cleaningJobID, reason.source, reason.varid.toInt, reason.args, repairValue))
          ajaxRequest(url, data, responseText => {
            val cleaningJobRepairRespons = upickle.read[NoResponse](responseText)
            /*t.modState(s => {
              val drow = s.dataTableState.data.data(s.dataTableViewState.offset + s.dataTableViewState.rows.indexOf(row)).data
              val dcol = repairValues.indexOf(repairValue) + s.latLonColsState.latColIdx
              drow(dcol).isDet = true
              drow(dcol).data = repairValue
              s.copy()
            })*/
            requestCleaningData(cleaningJobID)
          })
        }
      })
    }
    
    ///----------------------------------------
    ///-------Table with uncertainty code
    ///----------------------------------------
    def onTableCellClick(col:String, desc: String, tableRow:ODInTable.Model): Callback = {
      t.modState(s => { 
        val houseNumber = s"${tableRow.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt}"
        val streetName = s"${tableRow.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}"
        val city = s"${tableRow.data(s.addrColsState.cityIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}"
        val state = "NY"
        s.copy(selectedCol = Some(col), selectedRow = Some(tableRow), selectedTask = Some(s.taskListState.data(s.dataTableViewState.rows.indexOf(tableRow))), addressState = AddressState(houseNumber, streetName, city, state)) 
      }, t.state.flatMap( s => {
        UBOdinGoogleMap.geocodeAddress(s.addressState.houseNumber, s.addressState.streetName, s.addressState.city, s.addressState.state, openGeoResDialog)
      }))
  }
    
    
    def uncertaintyCellHighlight: Any => ReactElement =
      cell => {
        val (col, cellString, isDet, row) = cell match {
          case (col:String, CleaningJobDataCell(data, isDet), row:ODInTable.Model) => (col, data, isDet, row)
          case _ => (null, cell.toString, true, null)
        }
        if (!isDet)
          <.span(^.color := "red", ^.onClick --> onTableCellClick(col, cellString, row) )(cellString)
        else <.span(cellString)
      }
      
    def uncertaintyRowHighlight: Model => Option[String] = 
      row => row.isDet match {
        case true => None
        case false => Some("NonDetRowStyle")
      }
      
    def getMarkerFromModel(model:ODInTable.Model, latCol:Int, lonCol:Int, label:String) : Marker = {
      val latCell: CleaningJobDataCell = model.data(latCol).asInstanceOf[CleaningJobDataCell]
      val lonCell: CleaningJobDataCell = model.data(lonCol).asInstanceOf[CleaningJobDataCell]
      val latlng = LatLng(latCell.data.replaceAll("'", "").toDouble, lonCell.data.replaceAll("'", "").toDouble)
      Marker( position = latlng, title = label, content = s"<h3>$label</h3>" )
    }
    
    def pageChange(offset:Int, tableRows:Vector[ODInTable.Model]) : Callback = {
      println(s"pageChange: offset: $offset rowCount:${tableRows.length}")
      t.modState(s => s.copy( dataTableViewState = DataTableViewState(offset, tableRows), mapState = MapState( LatLng(42.8864 ,-78.8784), tableRows.map(row => getMarkerFromModel(row, s.latLonColsState.latColIdx, s.latLonColsState.lonColIdx, s"${row.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt} ${row.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}" ) ).toList , 10, s.drawerState.selected.contains("CLUSTER"))))
    }
      
    val afterNextPage: (Int, Vector[ODInTable.Model]) => Callback = {
      (offset, tableRows) => pageChange(offset, tableRows) >> requestCleaningJobTaskFocusedList(cleaningJobID, tableRows.flatMap(row => { 
        row match {
          case CleaningJobDataRow(_, _, prov) => Some(prov)
          case _ => None
        }
      }))
    }
    
    val afterPrevPage: (Int, Vector[ODInTable.Model]) => Callback = {
      (offset, tableRows) => pageChange(offset, tableRows) >> requestCleaningJobTaskFocusedList(cleaningJobID, tableRows.flatMap(row => { 
        row match {
          case CleaningJobDataRow(_, _, prov) => Some(prov)
          case _ => None
        }
      }))
    }
    
    val onTableRowSelect: ODInTable.Model => Callback = {
      tableRow => 
        t.modState(s => {
          val houseNumber = s"${tableRow.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt}"
          val streetName = s"${tableRow.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}"
          val city = s"${tableRow.data(s.addrColsState.cityIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}"
          val state = "NY"
          val rowIDs = s.dataTableViewState.rows.map(_.prov)
          s.copy(selectedTask = Some(s.taskListState.data(rowIDs.indexOf(tableRow.prov))), selectedRow = Some(tableRow), mapState = MapState( LatLng(tableRow.data(s.latLonColsState.latColIdx).data.replaceAll("'", "").toDouble, tableRow.data(s.latLonColsState.lonColIdx).data.replaceAll("'", "").toDouble), List(getMarkerFromModel(tableRow, s.latLonColsState.latColIdx, s.latLonColsState.lonColIdx, s"${tableRow.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt} ${tableRow.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}" )), 17, s.drawerState.selected.contains("CLUSTER")), addressState = AddressState(houseNumber, streetName, city, state))
        })
    }
    
    val loadTableData: (ReactMouseEvent, Boolean) => Callback = {
      (event, toggled) => requestCleaningData(cleaningJobID)
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
       reasons => e => t.modState(s => {
           val tableRow = s.dataTableViewState.rows(s.taskListState.data.indexOf(reasons))
           val houseNumber = s"${tableRow.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt}"
           val streetName = s"${tableRow.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}" 
           val city = s"${tableRow.data(s.addrColsState.cityIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}"
           val state = "NY"
           val rowIDs = s.dataTableViewState.rows.map(_.prov)
           s.copy(selectedTask = Some(reasons), selectedRow = Some(tableRow), mapState = MapState( LatLng(tableRow.data(s.latLonColsState.latColIdx).data.replaceAll("'", "").toDouble, tableRow.data(s.latLonColsState.lonColIdx).data.replaceAll("'", "").toDouble), List(getMarkerFromModel(tableRow, s.latLonColsState.latColIdx, s.latLonColsState.lonColIdx, s"${tableRow.data(s.addrColsState.houseNumberIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "").toDouble.toInt} ${tableRow.data(s.addrColsState.streetIdx).asInstanceOf[CleaningJobDataCell].data.replaceAll("'", "")}" )), 17, s.drawerState.selected.contains("CLUSTER")), addressState = AddressState(houseNumber, streetName, city, state))
         }, t.state.flatMap(s => UBOdinGoogleMap.geocodeAddress(s.addressState.houseNumber, s.addressState.streetName, s.addressState.city, s.addressState.state, openGeoResDialog)))
       
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
  
    val onNewRequest: (String,Int) => Callback =
      (chosen, idx) => updateAddressValues(3)(null, chosen)

    val updateAddressValues : Int => (ReactEvent, String) => Callback = 
      idx => (e, value) => t.modState(s => s.copy( addressState = {
        idx match{
          case 0 => s.addressState.copy(houseNumber = value)
          case 1 => s.addressState.copy(streetName = value)
          case 2 => s.addressState.copy(city = value)
          case 3 => s.addressState.copy(state = value)
        }
        
      }))

    val usStates = js.Array( "AL", "AK", "AS", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FM", "FL", "GA", "GU", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MH", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "MP", "OH", "OK", "OR", "PW", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VI", "VA", "WA", "WV", "WI", "WY" )
       
    def handleManualGPSLookup: TouchTapEvent => Callback = 
      e => popoverClose(e) >>  t.state.map(s => (s.addressState)).flatMap( address => UBOdinGoogleMap.geocodeAddress(address.houseNumber, address.streetName, address.city, address.state, manualLocationLookupResult))
    
    def manualLocationLookupResult(geoRes:UBOdinGoogleMap.GoogleGeocodingResult): Callback = t.modState(_.copy(gpsState = GPSLocationState(true, LatLng(42.8864 ,-78.8784), Some(LatLng(geoRes.results.head.geometry.location.lat,geoRes.results.head.geometry.location.lng))))) >> t.state.flatMap(s => { 
      s.drawerState.selectedWaitingCallback match {
        case Some(choice) => choice.settingOption match {
          case Some(LocationFilterSettingOption(distanceMeters, latCol, lonCol)) => requestCreateCleaningJobLocationFilter(s.dataTableState.data.cleaningJobID.toString, s.dataTableState.data.cleaningJobDataID.toString, choice.id, distanceMeters, latCol, lonCol, geoRes.results.head.geometry.location.lat,geoRes.results.head.geometry.location.lng)
          case _ => Callback.warn("Wrong menu option selected waiting for callback")
        }
        case _ => Callback.warn("No menu option selected waiting for callback")
      }
    })
    
    val usStateFilter : (String, String, String) => Boolean = MuiAutoCompleteFilters.caseInsensitiveFilter
    
    val selectDrawerItem: MainMenuChoice => TouchTapEvent => Callback = 
      choice => e => choice.settingOption match {
        case Some(LocationFilterSettingOption(distanceMeters, latCol, lonCol)) => t.state.flatMap(s => { 
          if(!s.drawerState.selected.contains(choice.id)){
            s.gpsState.location match {
              case Some(gpsLoc) => {
                requestCreateCleaningJobLocationFilter(s.dataTableState.data.cleaningJobID.toString, s.dataTableState.data.cleaningJobDataID.toString, choice.id, distanceMeters, latCol, lonCol, gpsLoc.lat, gpsLoc.lng)
              }
              case None => t.modState(s => s.copy(addressState = AddressState("","","",""), drawerState = s.drawerState.copy( selectedWaitingCallback = Some(choice)))) >> togglePopover("", "", 
                Some(<.div(
                    ^.padding := "20px",
                    <.h2("Manual Location Entry"),
                    <.p("Your location could not be determined.  We will look it up for you, if you enter your address."),
                    <.div(
                        MuiTextField( hintText = "House Number":ReactNode, floatingLabelText = "Enter House Number Here":ReactNode, onChange = updateAddressValues(0))(),
                        MuiTextField(hintText = "Street Name":ReactNode, floatingLabelText = "Enter Street Name Here":ReactNode, onChange = updateAddressValues(1))()   
                    ),
                    <.div(
                        MuiTextField(hintText = "City":ReactNode, floatingLabelText = "Enter City Here":ReactNode, onChange = updateAddressValues(2))(), 
                        MuiAutoComplete(
                          hintText = "State":ReactNode,
                          floatingLabelText = "Enter State Here":ReactNode,
                          filter            = usStateFilter,
                          dataSource        = usStates,
                          onNewRequest      = onNewRequest
                        )()
                    ),
                    <.div(
                        MuiFlatButton(key = "1", label = "Cancel", secondary = true, onTouchTap = popoverClose)(),
                        MuiFlatButton(key = "2", label = "OK, Look It Up!", secondary = true, onTouchTap = handleManualGPSLookup)()
                    )
                ))
              )
            }
          }
          else{
            requestRemoveCleaningJobLocationFilter(s.dataTableState.data.cleaningJobID.toString, s.dataTableState.data.cleaningJobDataID.toString, choice.id)
          }
        })
        case Some(MapClustererSettingOption()) =>  t.state.flatMap(s => {
          println(s"MapClustererSettingOption: ${s.drawerState.selected} contains ${choice.id}?")
          if(!s.drawerState.selected.contains(choice.id)){
            t.modState(s => s.copy(mapState = s.mapState.copy(cluster = true), drawerState = s.drawerState.copy( selected = s.drawerState.selected.union(Seq(choice.id)).distinct)))
          }
          else{
            t.modState(s => s.copy(mapState = s.mapState.copy(cluster = false), drawerState = s.drawerState.copy( selected = s.drawerState.selected.filter(!_.equals(choice.id)))))
          }
        })
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
        MuiFlatButton(key = "2", label = "Repair", secondary = true, onTouchTap = handleDialogRepair)(),
        MuiFlatButton(key = "3", label = "Acknowledge", secondary = true, onTouchTap = handleDialogAcknowledge)()
      )  
      
    def handleDialogCancel: TouchTapEvent => Callback =
    e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> togglePopover("","Repair Canceled")//Callback.info("Cancel Clicked")

    def handleDialogRepair: TouchTapEvent => Callback =
      e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> showSnack("Repair", "The data has been repaired")//Callback.info("Submit Clicked")
  
    def handleDialogAcknowledge: TouchTapEvent => Callback =
      e => t.modState(s => s.copy(dialogState = s.dialogState.copy(open = !s.dialogState.open))) >> showSnack("Acknowledge", "The data has been acknowledged")//Callback.info("Submit Clicked")
      
    def openDialog(title:String, message:String): Callback =
      t.modState(s => s.copy(dialogState = DialogState(true, title, message, None, None))) >> Callback.info("Opened")
   
    def handleDialogManualGPSRepairEntry: TouchTapEvent => Callback =
      e => t.modState(s => s.copy(repairValues = Seq())) >> togglePopover("", "", 
        Some(<.div(
            ^.padding := "20px",
            <.h2("Repair GPS Manually"),
            <.p("Please Enter GPS Coordinates:"),
            MuiTextField( hintText = "Latitude":ReactNode, floatingLabelText = "Enter Latitude Here":ReactNode, onChange = updateRepairValues(0))(),
            MuiTextField(hintText = "Longitude":ReactNode, floatingLabelText = "Enter Longitude Here":ReactNode, onChange = updateRepairValues(1))(),   
            MuiFlatButton(key = "1", label = "Cancel", secondary = true, onTouchTap = popoverClose)(),
            MuiFlatButton(key = "2", label = "Repair", secondary = true, onTouchTap = handleDialogManualGPSRepair)()
        ))
      )
    
    val updateRepairValues : Int => (ReactEvent, String) => Callback = 
      idx => (e, value) => t.modState(s => s.copy(repairValues = {
        println(s"update repair value: $idx -> $value")
        if(s.repairValues.length > idx) s.repairValues.zipWithIndex.map( idxEl => { if(idxEl._2 == idx) value else idxEl._1 })
        else s.repairValues.union(for(i <- s.repairValues.length-1 to idx) yield { if(i == idx) value else "" })
      }))
   
    def handleDialogManualGPSRepair:  TouchTapEvent => Callback = 
      e => popoverClose(e) >> t.state.map(s => (s.selectedRow, s.selectedTask, s.repairValues)).flatMap( stateSubset => handleDialogGPSRepair(stateSubset._2.get, stateSubset._1.get, stateSubset._3(0), stateSubset._3(1))(e))
    
    def handleDialogGPSRepair: (CleaningJobTaskGroup, ODInTable.Model, String, String) => TouchTapEvent => Callback =
      (reasons, row, lat, lon) => e => requestRepair(cleaningJobID, reasons, row, Seq( lat, lon)) >> t.modState(s => s.copy(dialogState = DialogState(!s.dialogState.open, s.dialogState.title, s.dialogState.message, None, None))) >> showSnack("Repaired", "The data has been repaired")//Callback.info("Submit Clicked")  
      
    def openGeoResDialog(geoRes:UBOdinGoogleMap.GoogleGeocodingResult): Callback = {
      t.state.flatMap( s =>
        {
          val tableRowOp = s.selectedRow
          val selectedAddress = s.addressState
          val selectedTask = s.selectedTask.get
          println(selectedTask)
          tableRowOp match { 
            case Some(tableRow) => {
              val stateDialogActions: ReactNode = js.Array(
                MuiFlatButton(key = "1", label = "Cancel", secondary = true, onTouchTap = handleDialogCancel)(),
                MuiFlatButton(key = "2", label = "Repair Manually", secondary = true, onTouchTap = handleDialogManualGPSRepairEntry)()
              )  
              val uncertainLat = tableRow.data(s.latLonColsState.latColIdx).data.replaceAll("'","")
              val uncertainLon = tableRow.data(s.latLonColsState.lonColIdx).data.replaceAll("'","")
              val content = <.div(
                <.h3("Location Repair Dialog"),
                <.div()(
                  <.div(^.display := "inline-block", ^.margin := "40px", ^.maxWidth := "40%", ^.verticalAlign := "top")(
                      <.h4("Uncertain Location: "),
                      <.div( s"${selectedAddress.houseNumber} ${selectedAddress.streetName} ${selectedAddress.city}, ${selectedAddress.state}" ),
                      <.div(  s"Latitude: ${uncertainLat} Longitude: ${uncertainLon}"),
                      MuiFlatButton(key = "3", label = "Acknowledge Current", secondary = true, onTouchTap = handleDialogGPSRepair(selectedTask, tableRow, uncertainLat, uncertainLon))()
                  ),
                  <.div(^.display := "inline-block", ^.margin := "40px", ^.maxWidth := "40%", ^.verticalAlign := "top")( geoRes.results.map(geoResult => {
                    <.div(
                        <.h4("Google Geocoded Location: "),
                        <.div( geoResult.formatted_address),
                        <.div(  s"Latitude: ${geoResult.geometry.location.lat} Longitude: ${geoResult.geometry.location.lng}"),
                        MuiFlatButton(key = "4", label = "Repair With Google Result", secondary = true, onTouchTap = handleDialogGPSRepair(selectedTask, tableRow, s"${geoResult.geometry.location.lat}", s"${geoResult.geometry.location.lng}"))()
                    )
                   })
                  ))
                )
              val pinColor = "FFE866" 
              val iconUrl = "https://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|" + pinColor
              val icon = Icon(iconUrl, Size(21, 34), Point(0,0), Point(10, 34))
              t.modState(s => s.copy(mapState = MapState(s.mapState.center, s.mapState.markers.union(List(Marker( position = LatLng(geoRes.results.head.geometry.location.lat, geoRes.results.head.geometry.location.lng), title = "", icon = icon, content = s"<h3>Google Geocoded ${selectedAddress.houseNumber} ${selectedAddress.streetName}</h3>" ))), s.mapState.zoom, false), dialogState = DialogState(true, "", "", Some(content), Some(stateDialogActions)))) >> Callback.info("Opened")
            }
            case None => Callback.info("No Row Selected not opening geo dialog")
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
      createWebSocket( None, Some((cleaningJobID, requestLoadCleaningJob)), Some((cleaningJobID, requestLoadCleaningJob)) )
      //requestGetCleaningJobSettingsOptions(cleaningJobID) >> requestGetCleaningJobSettings(cleaningJobID) >> requestCleaningJobTaskList(cleaningJobID, 10, 0) >> requestCleaningData(cleaningJobID)
    }
      
    //render  
    def render(P: RouterCtl[Page], S: State) = {
       println(s"render: ${ S.drawerState.selected } -> ${S.mapState.cluster}")
      val dialogTitle: ReactNode = js.Array(S.dialogState.title)
      
      val snackAction: ReactNode = js.Array("undo")
      
      if(!S.gpsState.enabled)  
        handleStartTracking(null)
      
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
          UBOdinMobileTearSheet(
            MuiList(key = "task_list")(
                S.taskListState.data.zipWithIndex.map(listElement => {
                  MuiListItem(
                      key = s"item${listElement._2}", 
                      primaryText = toElement(listElement._1),
                      leftIcon           = {if(S.selectedTask.equals(listElement._1))AlertErrorOutline()() else AlertWarning()()},
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
        
          S.cleaningJobType match{
            case "GIS" => { 
             <.div(Style.cleaningSection)(
                  <.div(Style.mapSection)(
                      UBOdinGoogleMap(width = "100%",center = S.mapState.center, markers = S.mapState.markers, zoom = S.mapState.zoom, cluster = S.mapState.cluster)
                  ),   
                  <.div(Style.cleaningTableSection)(
                      if(S.dataTableState.loaded)
                        ODInTable(data = S.dataTableState.data.data, columns = S.dataTableState.data.cols.toList, config = S.dataTableState.config, afterNextPage, afterPrevPage, Some(onTableRowSelect), Some(uncertaintyRowHighlight))
                      else
                        <.h4("loading...")  
                    )
              )
            }
            case "DATA" => {   
              <.div(Style.cleaningSection)(
                  <.div(Style.cleaningTableSection)(
                    if(S.dataTableState.loaded)
                      ODInTable(data = S.dataTableState.data.data, columns = S.dataTableState.data.cols.toList, config = S.dataTableState.config, afterNextPage, afterPrevPage, Some(onTableRowSelect), Some(uncertaintyRowHighlight))
                    else
                      <.h4("loading...")  
                  )
                )
            }
            case _ => <.div()
          },
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
  
  val component = ReactComponentB[RouterCtl[Page]]("CleaningJobPage")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount(_.backend.start)
    .build

  
    
  def apply(page:UBOdinAppRouter.CleaningJobPage, ctrl: RouterCtl[Page]) = {
    cleaningJobID = page.cleaningJobID.toString()
    println("UBOdinCleaningJobPage Apply: " + cleaningJobID)
    component(ctrl)
  }
 

}