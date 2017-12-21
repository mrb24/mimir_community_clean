package ubodin
import scalikejdbc._
import ubodin.Base64.Encoder

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.DefaultHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.server.WebSocketHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.ArrayList
import upickle._
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.sql.PreparedStatement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse

import mimir.MimirVizier
import mimir.serialization.Json
import mimir.exec.result.Row
import mimir.models.SourcedFeedbackT
import mimir.models.FeedbackSourceIdentifier
import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}
import mimir.algebra.NullPrimitive
import mimir.algebra.Operator
import mimir.parser.ExpressionParser
import mimir.algebra.Comparison
import mimir.algebra.Aggregate
import mimir.algebra.AggFunction
import mimir.algebra.Var

class MainServlet() extends HttpServlet {
  override def doPost(req : HttpServletRequest, resp : HttpServletResponse) = {
      val text = scala.io.Source.fromInputStream(req.getInputStream).mkString 
      println(req.getPathInfo)
      val routePattern = "\\/ajax\\/([a-zA-Z]+)".r
      req.getPathInfo match {
        case routePattern(route) => {
          try{
            val request = upickle.read[AjaxRequestWrapper](text)
            val responseObj = SharedServlet.handleRequest(route, request.request)
            val os = resp.getOutputStream()
            resp.setHeader("Content-type", "text/html");
            os.write(UBOdinAjaxResponse.writeResponse(responseObj).getBytes )
            os.flush()
            os.close() 
          } catch {
            case t: Throwable => {
              t.printStackTrace() 
              throw t
            }
          }
        }
        case _ => throw new Exception("request Not handled: " + req.getPathInfo)
      }  
  }
  override def doGet(req : HttpServletRequest, resp : HttpServletResponse) = {
    println(req.getPathInfo)
    
    val rootParhRegex = "\\/([\\w\\d_-]+)\\/.*".r
    var responseStr: String = ""
    
    req.getPathInfo match {
      case rootParhRegex(rootPath) => {
        rootPath match {
          case "plot" => {
            val devJobRegex = "\\/plot\\/([\\d.-]+)/(\\d+)/".r
            req.getPathInfo match {
              case devJobRegex(deviceID, cleaningJobID) => responseStr = SharedServlet.getCleaningJobDataPlot(deviceID, cleaningJobID, None, None)
            }
          }
          case _ => responseStr = UBOdinClientAppPage.skeleton().render;
        }
      }
      case _ => responseStr = UBOdinClientAppPage.skeleton().render;
    }
    
     
    
   
    val os = resp.getOutputStream()
            resp.setHeader("Content-type", "text/html");
            os.write(responseStr.getBytes() )
            os.flush()
            os.close()
            println("Response Written: " + responseStr.length())
  }
}

class MCCWebSocket extends WebSocketAdapter
{
    
    override def onWebSocketClose(statusCode:Int,  reason:String) : Unit = {
        super.onWebSocketClose(statusCode,reason);
        println("WebSocket Close: "+statusCode+" - "+reason);
    }

    override def  onWebSocketConnect( session:Session) : Unit = {
        super.onWebSocketConnect(session);
        println(s"WebSocket Connect: remote:${session.getRemoteAddress} protocol: ${session.getProtocolVersion} ");
    }

    override def onWebSocketError( cause:Throwable) : Unit = {
    	println("WebSocket Error "+cause);
    }

    override def  onWebSocketText( message:String) : Unit = {
        if (isConnected())
        {
          val request = upickle.read[WSRequestWrapper](message)
          println("WS Request -> " + request.deviceID + ": "+request.requestType)
          SharedServlet.webSocketConnections.get(request.deviceID) match {
            case None => SharedServlet.webSocketConnections(request.deviceID) = this
            case Some(ws) => {}
          }
            val routePattern = "([a-zA-Z]+)".r
            request.requestType match {
              case routePattern(route) => {
                try{
                  val responseObj = SharedServlet.handleRequest(route, request.request)
                  sendMessage(responseObj, Some(request.requestUID))
                } catch {
                  case t: Throwable => {
                    t.printStackTrace() 
                    throw t
                  }
                }
              }
              case _ => throw new Exception("request Not handled: " + request.deviceID + ": "+request.requestType)
            }  
            
        }
    }

    override def  onWebSocketBinary(arg0:Array[Byte], arg1:Int, arg2:Int) : Unit = {
    	println("binary message "+new String(arg0));
    }
    
    def sendMessage(msg:UBOdinAjaxResponse, requestUID:Option[Int]) : Unit = {
      //println(s"sending WS Message: ${requestUID}: ${msg}")
      getRemote().sendStringByFuture(upickle.write(WSResponseWrapper(msg.responseID, requestUID.getOrElse(-1), UBOdinAjaxResponse.writeResponse(msg))))
    }
}
  
object SharedServlet {
  val webSocketConnections = scala.collection.mutable.Map[String,MCCWebSocket]()
  
  def handleRequest(route:String, request:String) : UBOdinAjaxResponse = {
   
    val requestObj = UBOdinAjaxRequest.readRequest(route, request)
          
    requestObj match {
      case CleaningJobListRequest(deviceID) =>  {
        val cleaningJobs = getCleaningJobList(deviceID)
        (CleaningJobListResponse(Vector[CleaningJob]().union(cleaningJobs)))
      }
      case CleaningJobTaskListRequest(deviceID, cleaningJobID, operator, count, offset) =>  {
        val cleaningJobs = getCleaningJobTaskList(deviceID, cleaningJobID, operator, count, offset)
        (CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobs)))
      }
      case CleaningJobTaskListSchemaRequest(deviceID, cleaningJobID, operator, cols) =>  {
        val cleaningJobs = getCleaningJobTaskListSchema(deviceID, cleaningJobID, operator, cols)
        (CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobs))) 
      }
      case CleaningJobTaskFocusedListRequest(deviceID, cleaningJobID, operator, rowIds, cols) =>  {
        val cleaningJobs = getCleaningJobTaskFocusedList(deviceID, cleaningJobID, operator, rowIds, cols)
        (CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobs)))
      }
      case LoadCleaningJobsModerationRequest(deviceID) =>  {
        val cleaningJobsModerations = getCleaningJobsModeration(deviceID)
        CleaningJobsModerationResponse(cleaningJobsModerations)
      }
      case CleaningJobDataRequest(deviceID, cleaningJobID, operStack, count, offset) =>  {
        val cleaningJobData = getCleaningJobData(deviceID, cleaningJobID, operStack, count, offset)
        (CleaningJobDataResponse(cleaningJobData, count.getOrElse(5), offset.getOrElse(0)))
      }
      case RollUpCleaningJobDataRequest(deviceID, cleaningJobID, operator, filters, groupBy, aggrs) => {
        val cleaningJobData = getCleaningJobDataRollUp(deviceID, cleaningJobID, operator, filters, groupBy, aggrs)
        (CleaningJobDataResponse(cleaningJobData, 5, 0))
      }
      case CleaningJobDataCountRequest(deviceID, cleaningJobID) =>  {
        (CleaningJobDataCountResponse(getCleaningJobDataCount(deviceID, cleaningJobID)))
      }
      case CleaningJobDataAndCountRequest(deviceID, cleaningJobID, count, offset) =>  {
        val totalCount = getCleaningJobDataCount(deviceID, cleaningJobID)
        val data = getCleaningJobData(deviceID, cleaningJobID, Seq(), count, offset)
        (CleaningJobDataAndCountResponse(CleaningJobDataResponse(data, count.getOrElse(5), offset.getOrElse(0)), CleaningJobDataCountResponse(totalCount)))
      }
      case filterReq@CreateCleaningJobLocationFilterSettingRequest(deviceID, cleaningJobDataID, name, distance, latCol, lonCol, lat, lon) =>  {
        createCleaningJobLocationFilterSetting(filterReq)
        (NoResponse())
      }
      case filterReq@RemoveCleaningJobLocationFilterSettingRequest(deviceID, cleaningJobDataID, name) =>  {
        removeCleaningJobLocationFilterSetting(filterReq)
        (NoResponse())
      }
      case GetCleaningJobSettingsRequest(deviceID, cleaningJobID) =>  {
        val cleaningJobSettings = getCleaningJobSettings(deviceID, cleaningJobID)
        (GetCleaningJobSettingsResponse(cleaningJobSettings.toVector))
      }
      case GetCleaningJobSettingsOptionsRequest(cleaningJobID) =>  {
        val cleaningJobSettingsOptions = getCleaningJobSettingsOptions(cleaningJobID)
        (GetCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptions.toVector))
      }
      case LoadCleaningJobRequest(deviceID, cleaningJobID) =>  {
        val cleaningJobSettingsOptions = getCleaningJobSettingsOptions(cleaningJobID)
        val options = GetCleaningJobSettingsOptionsResponse(cleaningJobSettingsOptions.toVector)
        val cleaningJobSettings = getCleaningJobSettings(deviceID, cleaningJobID)
        val settings = GetCleaningJobSettingsResponse(cleaningJobSettings.toVector)
        val cleaningJobData = getCleaningJobData(deviceID, cleaningJobID, Seq(), Some(5), Some(0))
        val rowids = cleaningJobData.head.data.map(row => row.prov)
        val cleaningJobTasks = getCleaningJobTaskFocusedList (deviceID, cleaningJobID, cleaningJobData.head.operator.head, rowids, cleaningJobData.head.cols)
        val cleaningJobTasksSchema = getCleaningJobTaskListSchema(deviceID, cleaningJobID, cleaningJobData.head.operator.head, cleaningJobData.head.cols)
        val tasks = CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobTasks))
        val tasksSchema = CleaningJobTaskListResponse(Vector[CleaningJobTaskGroup]().union(cleaningJobTasksSchema))
        val data = CleaningJobDataResponse(cleaningJobData, 5, 0)
        println("getting count")
        val dataCount = CleaningJobDataCountResponse(1000)//getCleaningJobDataCount(deviceID, cleaningJobID))
        (LoadCleaningJobResponse(getCleaningJob(cleaningJobID), options, settings, tasks, tasksSchema, data, dataCount))
      }
      case UserInfoRequest(deviceID) => {
        (UserInfoResponse(getUserInfo(deviceID).head))
      }
      case SetDeviceLocationRequest(deviceID, lat, lon) => {
        setDeviceLocation(deviceID, lat, lon) 
        (NoResponse())
      }
      case LoginRequest(deviceID) => {
        val cleaningJobs = getCleaningJobList(deviceID)
        val userInfo = getUserInfo(deviceID) 
        userInfo match {
          case List() => (NoResponse())
          case _ => (LoginResponse(UserInfoResponse(userInfo.head), CleaningJobListResponse(Vector[CleaningJob]().union(cleaningJobs))))
        }
      }
      case ualr@UserActionLogRequest(deviceID, eventType, target, xCoord, yCoord, scroll, timestamp, extra) => {
        println(s"User Action Log: device:$deviceID, Event Type:$eventType, Target:$target, X:$xCoord, Y:$yCoord, Scroll:$scroll, Time:$timestamp, Res:$extra")
        logUserAction(ualr);
        (NoResponse())
      }
      case CleaningJobRepairRequest(deviceID, cleaningJobID, model, idx, args, repairValue) => {
        println(s"Repair: device:$deviceID, job:$cleaningJobID, model:$model, idx:$idx, args:$args, repair:$repairValue")
        MimirCommunityServer.feedbackSrc(deviceID, model, idx, args, repairValue);
        (NoResponse())
      }
      case CleaningJobModRepairRequest(deviceID, cleaningJobID, model, idx, args, repairValue) => {
        println(s"ModRepair: device:$deviceID, job:$cleaningJobID, model:$model, idx:$idx, args:$args, repair:$repairValue")
        MimirCommunityServer.feedback( model, idx, args, repairValue);
        (NoResponse())
      }
      case NoRequest() => {
        println("No Op Request")
        (NoResponse())
      }
      case x => {
        throw new Exception("request Not handled: " + x)
      }
    }
  }
  
  def getCleaningJobList(deviceID:String) : List[CleaningJob] = {
     NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOBS" // don't worry, prevents SQL injection
          .map(rs => CleaningJob(rs.int("CLEANING_JOB_ID"), rs.string("CLEANING_JOB_NAME"), rs.string("TYPE"), rs.string("IMAGE"), Stream(rs.string("CLEANING_JOB_NAME").toLowerCase())))
          .list                  // single, list, traversable
          .apply() 
        }
  }
  
  def getCleaningJob(cleaningJobID:String) : CleaningJob = {
     NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOBS WHERE CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => CleaningJob(rs.int("CLEANING_JOB_ID"), rs.string("CLEANING_JOB_NAME"), rs.string("TYPE"), rs.string("IMAGE"), Stream(rs.string("CLEANING_JOB_NAME").toLowerCase())))
          .single                  // single, list, traversable
          .apply().get 
        }
  }
  
  def getUserInfo(deviceID:String) : List[UserInfoData] = {
     val usersData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from USERS u where u.USER_ID = (SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1)" // don't worry, prevents SQL injection
          .map(rs => {
            val userSchedule = UserSchedule((NamedDB("mimir_community_server") autoCommit { implicit session =>
             sql"SELECT * FROM SCHEDULE_USERS su join SCHEDULE_CLEANING_JOBS sjs on sjs.SCHEDULE_CLEANING_JOBS_ID = su.SCHEDULE_CLEANING_JOBS_ID join CLEANING_JOBS js on js.CLEANING_JOB_ID = sjs.CLEANING_JOB_ID where su.USER_ID = ${rs.string("USER_ID")}"
              .map(rssub => {
                val startedTime = rssub.date("STARTED_TIME") match { case null => 0.0; case x => x.getTime.toDouble; }
                val endedTime = rssub.date("ENDED_TIME")  match { case null => 0.0; case x => x.getTime.toDouble; }
                UserScheduleItem(rssub.int("USER_ID"), CleaningJob(rssub.int("CLEANING_JOB_ID"), rssub.string("CLEANING_JOB_NAME"), rssub.string("TYPE"), rssub.string("IMAGE"), Stream(rssub.string("CLEANING_JOB_NAME").toLowerCase())), rssub.int("SCHEDULE_CLEANING_JOBS_ID"), /*rssub.date("START_TIME").getTime.toDouble, rssub.date("END_TIME").getTime.toDouble*/0, 0, startedTime, endedTime)
              })
              .list                  // single, list, traversable
              .apply() 
            }).toArray )
            val userDevices = (NamedDB("mimir_community_server") autoCommit { implicit session =>
             sql"select d.DEVICE_UID from  USER_DEVICES ud join DEVICES d on d.DEVICE_ID = ud.DEVICE_ID where ud.USER_ID = ${rs.string("USER_ID")}" // don't worry, prevents SQL injection
              .map(rssubd => rssubd.string("DEVICE_UID"))
              .list                  // single, list, traversable
              .apply() 
             }).toArray
            UserInfoData(rs.int("USER_ID"), rs.string("FIRST_NAME"), rs.string("LAST_NAME"), userDevices, userSchedule)
          })
          .list                  // single, list, traversable
          .apply() 
        } 
     if(usersData.isEmpty){
       //save new unregistered device UID
       if((NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select d.DEVICE_UID FROM DEVICES d where d.DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
          .map(rssubd => rssubd.string("DEVICE_UID"))
          .list                  // single, list, traversable
          .apply() 
         }).isEmpty){
           val deviceDBID = NamedDB("mimir_community_server") autoCommit  { implicit session =>
             sql"INSERT INTO DEVICES ( DEVICE_UID) VALUES( ${deviceID})" // don't worry, prevents SQL injection
              .updateAndReturnGeneratedKey 
              .apply() 
           }
           val newUserID = createNewUser("User",deviceID)
           NamedDB("mimir_community_server") autoCommit  { implicit session =>
             sql"INSERT INTO USER_DEVICES ( USER_ID, DEVICE_ID ) VALUES (${newUserID}, ${deviceDBID})" // don't worry, prevents SQL injection
              .execute() 
              .apply() 
           }
           assignAllCleaningJobToUser(newUserID)
       }
       getUserInfo(deviceID)
     }
     else
       usersData
  }
  
  def getUserName(deviceID:String) : String = {
     NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from USERS u where u.USER_ID = (SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1)" // don't worry, prevents SQL injection
          .map(rs => {
            s"${rs.string("FIRST_NAME")} ${rs.string("LAST_NAME")}"
          })
          .single                  // single, list, traversable
          .apply().get 
        } 
  }
  
  def createNewUser(firstName:String,lastName:String) : Long = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       sql"INSERT INTO USERS ( FIRST_NAME, LAST_NAME ) VALUES ( ${firstName},${lastName} )" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply() 
     }  
  }
                                                                                                            //when starship Enterprise (TNG) launches
  def assignCleaningJobToUser(userID:Long,cleaningJobScheduleID:Long,startTime:String = "2017-08-13 00:00:00", endTime:String = "2363-01-01 00:00:00") : Long = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       sql"INSERT INTO SCHEDULE_USERS ( USER_ID, SCHEDULE_CLEANING_JOBS_ID, START_TIME, END_TIME ) VALUES ( ${userID},${cleaningJobScheduleID},${startTime},${endTime} )" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply() 
     }
  }
  
  def assignAllCleaningJobToUser(userID:Long, startTime:String = "2017-08-13 00:00:00", endTime:String = "2363-01-01 00:00:00") : Seq[Long] = {
    (NamedDB("mimir_community_server") autoCommit  { implicit session =>
       sql"SELECT SCHEDULE_CLEANING_JOBS_ID FROM SCHEDULE_CLEANING_JOBS" // don't worry, prevents SQL injection
       .map(rs => rs.long("SCHEDULE_CLEANING_JOBS_ID")) 
       .list
       .apply() 
     }).map(schedID => assignCleaningJobToUser(userID,schedID,startTime,endTime))
  }
  
  private def containRepair(repair:mimir.ctables.Repair) : CleaningJobRepairContainer = {
    repair match {
      case mimir.ctables.RepairFromList(_) => CleaningJobTaskRepair.containRepair("RepairFromList", upickle.read[CleaningJobTaskRepairFromList](repair.toJSON))
      case mimir.ctables.RepairByType(_) => CleaningJobTaskRepair.containRepair("RepairByType", upickle.read[CleaningJobTaskRepairByType](repair.toJSON))
      case mimir.ctables.ModerationRepair(_) => CleaningJobTaskRepair.containRepair("ModerationRepair", upickle.read[CleaningJobTaskModerationRepair](repair.toJSON))
    }
  }
  
  def getCleaningJobTaskList(deviceID:String, cleaningJobID:String, operator:String, count:Int, offset:Int) : List[CleaningJobTaskGroup] = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val reasonSeqList = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cj.CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => {
            MimirCommunityServer.explainEverything(Json.toOperator(Json.parse(operator))/*assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))*/).map(reasonSet => {
               reasonSet.take(MimirVizier.db, count, offset).map(reason => { 
                 val containedRepair = containRepair(reason.repair)
                 CleaningJobTaskGroup( List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containedRepair, reason.args.map( _.toString).toList )))
               })
             }).flatten
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
        reasonSeqList.flatten
   }
  
   def getCleaningJobTaskListSchema(deviceID:String, cleaningJobID:String, operator:String, cols:Seq[String]) : List[CleaningJobTaskGroup] = {
     mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
     val reasonSeqList = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cj.CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => {
            //get all schema level reasons and put each in a seperate group
            val oper = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
            MimirCommunityServer.explainSchema(oper, cols).map(reasonSet => {
             reasonSet.all(MimirVizier.db).map(reason => { 
               CleaningJobTaskGroup(List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containRepair(reason.repair), reason.args.map( _.toString).toList )))
             })}).flatten.toList 
          }).list.apply()
        }
     mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
     reasonSeqList.flatten
   }
   
  def getCleaningJobTaskFocusedList(deviceID:String, cleaningJobID:String, operator:String, rows:Seq[String], cols:Seq[String]) : List[CleaningJobTaskGroup] = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val reasonSeqList = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cj.CLEANING_JOB_ID = $cleaningJobID" // don't worry, prevents SQL injection
          .map(rs => {
            rs.string("TYPE") match {
              case "GIS" => {
                //for gis we want to group the lat and lon repairs so the can be repaired in one go, so the cols here should be the lat and lon cols
                val oper = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
                rows.map(row => CleaningJobTaskGroup(MimirCommunityServer.explainSubsetWithoutSchema(oper, Seq(row), cols).map(reasonSet => {
                 reasonSet.all(MimirVizier.db).map(reason => { 
                   //upickle.read[CleaningJobTask](reason.toJSON)
                   CleaningJobTask(reason.model.name, reason.idx, reason.reason, containRepair(reason.repair), reason.args.map( _.toString).toList )
                 })
               }).flatten.toList))
              }
              case "DATA" | "INTERACTIVE" => {
                //for this, each reason becomes it's own task group
                val oper = Json.toOperator(Json.parse(operator))
                MimirCommunityServer.explainSubsetWithoutSchema(oper/*assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))*/, rows, cols).map(reasonSet => {
                 reasonSet.all(MimirVizier.db).map(reason => { 
                   //CleaningJobTaskGroup( List(upickle.read[CleaningJobTask](reason.toJSON)))
                   CleaningJobTaskGroup( List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containRepair(reason.repair), reason.args.map( _.toString).toList )))
                 })
               }).flatten
              }
            }
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
        reasonSeqList.flatten
   }
  
  def getCleaningJobsModeration(deviceID:String) : List[(String, Vector[CleaningJobTaskGroup])] = {
    val processedFeedback = scala.collection.mutable.Set[(String, FeedbackSourceIdentifier, Int, Seq[mimir.algebra.PrimitiveValue], Seq[mimir.algebra.PrimitiveValue])]()
    val processedQueries = scala.collection.mutable.Set[String]()
    (NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cjd.*, cj.TYPE from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj on cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID" // don't worry, prevents SQL injection
          .map(rs => {
            val query = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
            if(!processedQueries.contains(query.toString)){
              processedQueries.add(query.toString)
              MimirCommunityServer.explainEverything(query).map(reasonSet => {
                 (rs.string("CLEANING_JOB_ID"), reasonSet.all(MimirVizier.db).flatMap(reason => { 
                  (reason.model match {
                     case sourcedFeedbackModel:SourcedFeedbackT[Any] => {
                       sourcedFeedbackModel.feedbackSources.filter(s => !s.equals(mimir.models.FeedbackSource.groundSource)).flatMap(fbs => {
                         if(!processedFeedback.contains((sourcedFeedbackModel.name, fbs, reason.idx, reason.args, reason.hints))){
                           processedFeedback.add((sourcedFeedbackModel.name, fbs, reason.idx, reason.args, reason.hints))
                           mimir.models.FeedbackSource.feedbackSource = fbs
                           val srcReason = new mimir.ctables.ModelReason(sourcedFeedbackModel, reason.idx, reason.args, reason.hints)
                           val cjt = srcReason.confirmed match {
                             case true if !sourcedFeedbackModel.hasGroundFeedback(reason.idx, reason.args) =>  {
                               val modRepair = mimir.ctables.ModerationRepair("\"" +sourcedFeedbackModel.getFeedback(reason.idx, reason.args).get.asString + "\"")
                               Some(CleaningJobTask(srcReason.model.name, srcReason.idx, srcReason.reason, containRepair(modRepair), srcReason.args.map( _.toString).toList ))
                             }
                             case _ => None
                           }
                           mimir.models.FeedbackSource.feedbackSource = FeedbackSourceIdentifier()
                           cjt
                         }
                         else None
                       }).toList
                     }
                     case _ => reason.confirmed match {
                           case true =>  {
                             List(CleaningJobTask(reason.model.name, reason.idx, reason.reason, containRepair(reason.repair), reason.args.map( _.toString).toList ))
                           }
                           case false => List()
                         }
                   }) match {
                     case List() => None
                     case cjtl:List[CleaningJobTask] => Some(CleaningJobTaskGroup(cjtl))
                   }
                 }).toVector)
               })
            } else Vector()
          }).list.apply()
        }).flatten
  }
  
  def getCleaningJobData(deviceID:String, cleaningJobID:String, operStack:Seq[String], count:Option[Int], offset:Option[Int]) : Vector[CleaningJobData] = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val cleaningJobData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
             val query = operStack match {
               case Seq() => assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
               case _ => Json.toOperator(Json.parse(operStack.last))
             }
             val oper = count match {
               case Some(limit) => query.limit(limit, offset.getOrElse(0))
               case None => query
             }
             val newQueryStack = operStack match {
               case Seq() => Seq(Json.ofOperator(query).toString)
               case x => x
             }
             val queryRes = MimirCommunityServer.queryMimir(oper)
             CleaningJobData(rs.int("CLEANING_JOB_DATA_ID"), rs.int("CLEANING_JOB_ID"), newQueryStack, queryRes._1, queryRes._2)
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
        cleaningJobData.toVector
  }
  
  def getCleaningJobDataRollUp(deviceID:String, cleaningJobID:String, operator:Seq[String], filters:Seq[SliceDiceFilter], groupBy:Set[String], aggrs:Seq[SliceDiceAggr]): Vector[CleaningJobData] = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val cleaningJobData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
             val query = Json.toOperator(Json.parse(operator.last))
             val sliceQuery = filters match {
               case Seq() => query
               case x => x.foldLeft(query)((oper,filter) => {
                 oper.filter(mimir.parser.ExpressionParser.expr(s"${filter.col} ${filter.op} ${filter.value}"))
               })
             }
             val rollupQuery = (groupBy.toSeq, aggrs) match {
               case (Seq(), Seq()) => sliceQuery
               case (Seq(), _) => Aggregate(Seq(), aggrs.map(aggr => AggFunction(aggr.op,false,Seq(Var(aggr.col)),s"${aggr.op}_${aggr.col}")), sliceQuery)
               case (gbs, Seq()) => Aggregate(gbs.map(Var(_)), Seq(), sliceQuery)
               case (gbs, _) => Aggregate(gbs.map(Var(_)), aggrs.map(aggr => AggFunction(aggr.op,false,Seq(Var(aggr.col)),s"${aggr.op}_${aggr.col}")), sliceQuery) 
             }
             val oper = rollupQuery.limit(5, 0)
             val queryRes = MimirCommunityServer.queryMimir(oper)
             CleaningJobData(rs.int("CLEANING_JOB_DATA_ID"), rs.int("CLEANING_JOB_ID"), operator :+ Json.ofOperator(rollupQuery).toString, queryRes._1, queryRes._2)
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
    cleaningJobData.toVector
  }
  
  def getCleaningJobDataOperator(deviceID:String, cleaningJobID:String, count:Option[Int], offset:Option[Int]) : Vector[String] = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val cleaningJobData = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
             val query = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
             mimir.serialization.Json.ofOperator(count match {
               case Some(limit) => query.limit(limit, offset.getOrElse(0))
               case None => query
             }).toString
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
        cleaningJobData.toVector
  }
  
  def getCleaningJobDataPlot(deviceID:String, cleaningJobID:String, xCol:Option[String], yCol:Option[String]) : String = {
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier(deviceID, getUserName(deviceID)))
    val cleaningJobDataPlot = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select cj.CLEANING_JOB_NAME, cjd.* from CLEANING_JOB_DATA cjd join CLEANING_JOBS cj ON cj.CLEANING_JOB_ID = cjd.CLEANING_JOB_ID WHERE cjd.CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
             val oper = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
             val plotProjectArgs = Seq(
               xCol match {
                 case None => mimir.algebra.ProjectArg("xval", mimir.algebra.Var(oper.columnNames.head))
                 case Some(col) => mimir.algebra.ProjectArg("xval", mimir.algebra.Var(col))
               },
               yCol match {
                 case None => mimir.algebra.ProjectArg("yval", mimir.algebra.Var(oper.columnNames.last))
                 case Some(col) => mimir.algebra.ProjectArg("yval", mimir.algebra.Var(col))
               })
             
             val plotOper = mimir.algebra.Project(plotProjectArgs , oper)
             val plotRes = MimirCommunityServer.queryMimirToJson(plotOper)
             UBOdinPlotGenerator.getPlotHtml(plotRes, "curveStepAfter", rs.string("CLEANING_JOB_NAME"))
          }).list.apply()
        }
    mimir.models.FeedbackSource.setSource(FeedbackSourceIdentifier())
    cleaningJobDataPlot.head
  }
  
  def getCleaningJobDataCount(deviceID:String, cleaningJobID:String) : Int = {
    val cleaningJobDataCount = NamedDB("mimir_community_server") autoCommit { implicit session =>
         sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_ID = $cleaningJobID"// don't worry, prevents SQL injection
           .map(rs => {
             val query = assembleCleaningJobDataQuery(deviceID, rs.int("CLEANING_JOB_DATA_ID"))
             val oper = query.count(false)
             val queryRes = MimirCommunityServer.queryMimir(oper)
             queryRes._2.head.data.head.data.toInt
          }).single.apply()
        }
        cleaningJobDataCount.get
  }
  
  /*def assembleCleaningJobDataQuery(deviceID:String, cleaningJobDataID:Int) : String = {
    NamedDB("mimir_community_server") autoCommit { implicit session =>
      sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_DATA_ID = $cleaningJobDataID" // don't worry, prevents SQL injection
        .map(rs => {
          val userCleaningJobSettings = NamedDB("mimir_community_server") autoCommit { implicit session =>
           sql"Select s.* from DEVICES d join USER_DEVICES ud on ud.DEVICE_ID = d.DEVICE_ID join USERS u on u.USER_ID = ud.USER_ID join USER_CLEANING_JOB_SETTINGS us on us.USER_ID = u.USER_ID join CLEANING_JOB_SETTINGS s on s.CLEANING_JOB_SETTINGS_ID = us.CLEANING_JOB_SETTING_ID where d.DEVICE_UID = $deviceID AND s.CLEANING_JOB_DATA_ID = ${rs.int("CLEANING_JOB_DATA_ID")}"
            .map(rssub => {
              val settingType = rssub.string("TYPE") 
              val settingJson = rssub.string("SETTING")
              CleaningJobSetting.readSetting(settingType, settingJson)
            })
            .list 
            .apply() 
          }
          userCleaningJobSettings.foldLeft(rs.string("QUERY") + " WHERE 1 = 1")((init, setting) => {
            val settingPart = setting match {
              case CleaningJobFilterSetting(_, _, queryWhere) => s" AND $queryWhere"
              case _ => ""
            }
            init + settingPart
          })
        }).single.apply().get
     }
  }*/
  
  def assembleCleaningJobDataQuery(deviceID:String, cleaningJobDataID:Int) : Operator = {
    NamedDB("mimir_community_server") autoCommit { implicit session =>
      sql"select * from CLEANING_JOB_DATA WHERE CLEANING_JOB_DATA_ID = $cleaningJobDataID" // don't worry, prevents SQL injection
        .map(rs => {
          val userCleaningJobSettings = NamedDB("mimir_community_server") autoCommit { implicit session =>
           sql"Select s.* from DEVICES d join USER_DEVICES ud on ud.DEVICE_ID = d.DEVICE_ID join USERS u on u.USER_ID = ud.USER_ID join USER_CLEANING_JOB_SETTINGS us on us.USER_ID = u.USER_ID join CLEANING_JOB_SETTINGS s on s.CLEANING_JOB_SETTINGS_ID = us.CLEANING_JOB_SETTING_ID where d.DEVICE_UID = $deviceID AND s.CLEANING_JOB_DATA_ID = ${rs.int("CLEANING_JOB_DATA_ID")}"
            .map(rssub => {
              val settingType = rssub.string("TYPE") 
              val settingJson = rssub.string("SETTING")
              CleaningJobSetting.readSetting(settingType, settingJson)
            })
            .list 
            .apply() 
          }
          val oper = Json.toOperator(Json.parse(rs.string("QUERY")))
          userCleaningJobSettings.foldLeft(oper)((init, setting) => {
            setting match {
              case CleaningJobFilterSetting(_, _, queryWhere) => oper.filter(ExpressionParser.expr(queryWhere)) 
              case _ => oper
            }
          })
        }).single.apply().get
     }
  }
  
  def createCleaningJobLocationFilterSetting(filterRequest: CreateCleaningJobLocationFilterSettingRequest) : (Long, Long) = {
    MimirCommunityServer.setLocationMimir(filterRequest.lat, filterRequest.lon)
    val filterSetting = upickle.write(CleaningJobFilterSetting(0, filterRequest.cleaningJobDataID.toInt, s"DST(CAST(${filterRequest.latCol} AS real), CAST(${filterRequest.lonCol} AS real), ${filterRequest.lat}, ${filterRequest.lon}) <= ${filterRequest.distance}"))
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       val cleaningJobSettingsID = sql"INSERT INTO CLEANING_JOB_SETTINGS ( CLEANING_JOB_DATA_ID, TYPE, NAME, SETTING) VALUES(${filterRequest.cleaningJobDataID}, ${"FILTER"}, ${filterRequest.name}, $filterSetting)" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply() 
       val userID = sql"SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${filterRequest.deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1"
        .map(rs => { rs.int("USER_ID") })
        .single
        .apply().get
       val userCleaningJobSettingsID = sql"INSERT INTO USER_CLEANING_JOB_SETTINGS (USER_ID, CLEANING_JOB_SETTING_ID) VALUES( ${userID}, ${cleaningJobSettingsID})" // don't worry, prevents SQL injection
        .updateAndReturnGeneratedKey 
        .apply()   
        (cleaningJobSettingsID, userCleaningJobSettingsID)
     }
  }
  
  def removeCleaningJobLocationFilterSetting(filterRequest: RemoveCleaningJobLocationFilterSettingRequest) : Unit = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
       val userID = sql"SELECT USER_ID from USER_DEVICES where DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${filterRequest.deviceID}) ORDER BY USER_DEVICE_ID desc LIMIT 1"
        .map(rs => { rs.int("USER_ID") })
        .single
        .apply().get
       val cleaningJobSettingsID = sql"SELECT CLEANING_JOB_SETTINGS_ID from CLEANING_JOB_SETTINGS WHERE CLEANING_JOB_DATA_ID = ${filterRequest.cleaningJobDataID} AND NAME = ${filterRequest.name} ORDER BY CLEANING_JOB_SETTINGS_ID desc LIMIT 1"
        .map(rs => { rs.int("CLEANING_JOB_SETTINGS_ID") })
        .single
        .apply().get
       sql"DELETE FROM CLEANING_JOB_SETTINGS WHERE CLEANING_JOB_DATA_ID = ${filterRequest.cleaningJobDataID} AND NAME = ${filterRequest.name}" // don't worry, prevents SQL injection
        .update 
        .apply() 
       
       sql"DELETE FROM USER_CLEANING_JOB_SETTINGS WHERE USER_ID = ${userID} AND CLEANING_JOB_SETTING_ID = ${cleaningJobSettingsID}" // don't worry, prevents SQL injection
        .update
        .apply()   
     }
  }
  
    
  def getCleaningJobSettings(deviceID:String, cleaningJobID:String): List[CleaningJobSettingContainer] = {
    getSavedDeviceLocation(deviceID)
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
      sql"SELECT cjs.TYPE, cjs.NAME, cjs.SETTING from CLEANING_JOB_DATA cjd join CLEANING_JOB_SETTINGS cjs on cjs.CLEANING_JOB_DATA_ID = cjd.CLEANING_JOB_DATA_ID join USER_CLEANING_JOB_SETTINGS ucjs on ucjs.CLEANING_JOB_SETTING_ID = cjs.CLEANING_JOB_SETTINGS_ID join USER_DEVICES ud on ud.USER_ID = ucjs.USER_ID join DEVICES d on d.DEVICE_ID = ud.DEVICE_ID WHERE d.DEVICE_UID = ${deviceID} and cjd.CLEANING_JOB_ID = ${cleaningJobID}"
        .map(rs => { CleaningJobSettingContainer(rs.string("NAME"), rs.string("TYPE"), rs.string("SETTING")) })
        .list
        .apply()
    }
  }
  
  def getCleaningJobSettingsOptions(cleaningJobID:String): List[SettingOptionContainer] = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
      sql"SELECT cjso.TYPE, cjso.NAME, cjso.ID, cjso.OPTION FROM CLEANING_JOB_DATA cjd join CLEANING_JOB_SETTINGS_OPTIONS cjso on cjso.CLEANING_JOB_DATA_ID = cjd.CLEANING_JOB_DATA_ID WHERE cjd.CLEANING_JOB_ID = ${cleaningJobID}"
        .map(rs => { SettingOptionContainer(rs.string("NAME"), rs.string("ID"), rs.string("TYPE"), rs.string("OPTION")) })
        .list
        .apply()
    }
  }
  
  def setDeviceLocation(deviceID:String, lat:Double, lon:Double) : Unit = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"UPDATE DEVICES SET LATITUDE = $lat, LONGITUDE = $lon WHERE DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
            .execute                
            .apply() 
           }
    MimirCommunityServer.setLocationMimir(lat, lon)
  }
  
  def getSavedDeviceLocation(deviceID:String) : (Double, Double) = {
    val savedLocation = NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"SELECT LATITUDE, LONGITUDE FROM DEVICES WHERE DEVICE_UID = ${deviceID}" // don't worry, prevents SQL injection
            .map(rs => { (rs.double("LATITUDE"), rs.double("LONGITUDE")) })
            .single                
            .apply().get 
           }
    MimirCommunityServer.setLocationMimir(savedLocation._1, savedLocation._2)
    savedLocation
  }
  
  def logUserAction(userAction:UserActionLogRequest) : Unit = {
    NamedDB("mimir_community_server") autoCommit  { implicit session =>
           sql"""INSERT INTO USER_ACTION_LOG ( USER_ID, DEVICE_ID, USER_ACTION ) VALUES (
                    (SELECT USER_ID FROM USER_DEVICES u WHERE u.DEVICE_ID = (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${userAction.deviceID})),
                    (SELECT DEVICE_ID FROM DEVICES WHERE DEVICE_UID = ${userAction.deviceID}),
                    ${upickle.write(userAction)} )""" // don't worry, prevents SQL injection
            .execute                
            .apply() 
           }
  }
  
  def list(path: String) = {
    val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
    val files =
      Option(new java.io.File("./" + dir).listFiles())
        .toSeq.flatten
    for{
      f <- files
      if f.getName.startsWith(last)
    } yield (f.getName, f.length())
  }
}


object MimirCommunityServer {
  def main(args: Array[String]) : Unit = {
    DatabaseManager.connectDatabases()
     
    val server = runWebService()
    
    while (System.in.read() != -1) {
      try {
        Thread.sleep(1000)
      } catch {
        case t: Throwable => println("MimirCommunityServer: sleep interrupted: " + t.toString())
      }
    }
    println("Shutting Down Server...")
    running = false
    mimirThread.interrupt()
    server.stop()
    DatabaseManager.shutdownDatabase()
    
  }
  
  def runWebService() : Server = {
    //startSSLServer(new MainServlet())
    startServer(new MainServlet())
  }
  
  
  def startServer(theServlet: HttpServlet) : Server = {
    val server = new Server(8089)
    
    val http_config = new HttpConfiguration();
    server.addConnector(new ServerConnector( server,  new HttpConnectionFactory(http_config)) );
    
    val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./app/js/resources")
		val contextHandler = new ContextHandler("/app");
    contextHandler.setResourceBase("./app/js/resources");
    contextHandler.setHandler(resource_handler);
    val resource_handler2 = new ResourceHandler()
		resource_handler2.setDirectoriesListed(true)
		resource_handler2.setResourceBase("./app/js/target/scala-2.11")
    val contextHandler2 = new ContextHandler("/js");
    contextHandler2.setResourceBase("./app/js/target/scala-2.11");
    contextHandler2.setHandler(resource_handler2);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");
    
    val wsHandler = new WebSocketServlet()
	    {
	        override def configure(factory:WebSocketServletFactory )
	        {
	            factory.getPolicy().setIdleTimeout(500000);
	            factory.register(classOf[MCCWebSocket]);
	        }
	    }
    val wsholder = new ServletHolder(wsHandler);
    servletContextHandler.addServlet(wsholder, "/ws/*");
    
    val handlerList = new HandlerCollection();
    handlerList.setHandlers( Array[Handler](contextHandler, contextHandler2, servletContextHandler, new DefaultHandler()));
    
    server.setHandler(handlerList);
    server.start()
    
     mimirThread.start()
     server
  }
  
  def startSSLServer(theServlet: HttpServlet) : Server = {
    val contextFactory = new SslContextFactory( );
    contextFactory.setKeyStorePath("websoc.jks");
    contextFactory.setKeyStorePassword("emsys01");
    contextFactory.setKeyManagerPassword("emsys01");
    contextFactory.setKeyStoreType("JKS");
    //contextFactory.setTrustStorePath("websoc_ts.jks");
    //contextFactory.setTrustStorePassword("emsys01");
    //contextFactory.setTrustStoreType("JKS");
    //contextFactory.setNeedClientAuth(true);
    
    val server = new Server();
    val http_config = new HttpConfiguration();
    http_config.setSecureScheme("https");
    http_config.setSecurePort(8089);
    http_config.setOutputBufferSize(32768);
    val https_config = new HttpConfiguration(http_config);
    val src = new SecureRequestCustomizer();
    src.setStsMaxAge(2000);
    src.setStsIncludeSubDomains(true);
    https_config.addCustomizer(src);
    
    val https = new ServerConnector(server, new SslConnectionFactory(contextFactory,HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(https_config));
    https.setPort(8089);
    https.setIdleTimeout(500000);
    
    /*val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./")
		val contextHandler = new ContextHandler("/");
    contextHandler.setResourceBase("./");
    contextHandler.setHandler(resource_handler);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/ajax");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");*/*/
    
    val resource_handler = new ResourceHandler()
		resource_handler.setDirectoriesListed(true)
		resource_handler.setResourceBase("./app/js/resources")
		val contextHandler = new ContextHandler("/app");
    contextHandler.setResourceBase("./app/js/resources");
    contextHandler.setHandler(resource_handler);
    val resource_handler2 = new ResourceHandler()
		resource_handler2.setDirectoriesListed(true)
		resource_handler2.setResourceBase("./app/js/target/scala-2.11")
    val contextHandler2 = new ContextHandler("/js");
    contextHandler2.setResourceBase("./app/js/target/scala-2.11");
    contextHandler2.setHandler(resource_handler2);
     
    val servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setContextPath("/");
    val holder = new ServletHolder(theServlet);
    servletContextHandler.addServlet(holder, "/*");
    
    val wsHandler = new WebSocketServlet()
	    {
	        override def configure(factory:WebSocketServletFactory )
	        {
	            factory.getPolicy().setIdleTimeout(500000);
	            factory.register(classOf[MCCWebSocket]);
	        }
	    }
    val wsholder = new ServletHolder(wsHandler);
    servletContextHandler.addServlet(wsholder, "/ws/*");
    
    val handlerList = new HandlerCollection();
    handlerList.setHandlers( Array[Handler](contextHandler, contextHandler2, servletContextHandler, new DefaultHandler()));
    
    server.addConnector(https);
    server.setHandler(handlerList);
    server.start()
    
    mimirThread.start()
    
    server
  }
  
  def explainCell(query:String, col:Int, row:String): Seq[String] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.Reason]](new MimirRunnable[Seq[mimir.ctables.Reason] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainCell(query, col, row))
      }
    })
    
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.Reason]) => ret.map(reason => reason.toJSON)
    }
  }
  
  def explainEverything(query:String): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainEverything(query))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainEverything(oper:mimir.algebra.Operator): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainEverything(oper))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSubset(oper:Operator, rows:Seq[String], cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        println(s"explainSubset ----------:\noper:$oper\nrows:$rows\ncols:$cols\n----------------------")
        returnedValue = Some(MimirVizier.explainSubset(oper, rows, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSubset(query:String, rows:Seq[String], cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSubset(query, rows, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSubsetWithoutSchema(query:String, rows:Seq[String], cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSubsetWithoutSchema(query, rows, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSubsetWithoutSchema(query:Operator, rows:Seq[String], cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSubsetWithoutSchema(query, rows, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSchema(query:String, cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSchema(query, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def explainSchema(query:Operator, cols:Seq[String]): Seq[mimir.ctables.ReasonSet] = {
    val reasons = mimirThread.runOnThread[Seq[mimir.ctables.ReasonSet]](new MimirRunnable[Seq[mimir.ctables.ReasonSet] ](){
      def run() = {
        returnedValue = Some(MimirVizier.explainSchema(query, cols))
      }
    })
    reasons match {
      case None => Seq()
      case Some(ret:Seq[mimir.ctables.ReasonSet]) => ret
    }
  }
  
  def feedback( model:String, idx:Int, args:Seq[String], repairValue:String): Unit = {
    val reasons = mimirThread.runOnThread[Boolean](new MimirRunnable[Boolean](){
      def run() = {
        doFeedback(model, idx, args, repairValue)
        returnedValue = None
      }
    })
    println("")
  }
  
  private def doFeedback( model:String, idx:Int, args:Seq[String], repairValue:String): Unit = {
    val argString = 
      if(!args.isEmpty){
        " (" + args.mkString(",") + ")"
      } else { "" }
    val feedbackStr = s"FEEDBACK ${model} ${idx}$argString IS ${ repairValue }"
    println(feedbackStr)
    MimirVizier.db.update(MimirVizier.db.parse(feedbackStr).head)
    println(s"feedback given by: ${mimir.models.FeedbackSource.feedbackSource}")
  }
  
  def setLocationMimir( lat:Double, lon:Double): Unit = {
    val reasons = mimirThread.runOnThread[Boolean](new MimirRunnable[Boolean](){
      def run() = {
        mimir.sql.sqlite.MeToLocationDistance.myLat = Some(lat)
        mimir.sql.sqlite.MeToLocationDistance.myLon = Some(lon) 
        returnedValue = None
      }
    })
    println("")
  }
  
  def queryMimirToJson(query:String): String = {
    val oper = MimirVizier.parseQuery(query)
    queryMimirToJson(oper)   
  }
  
  def queryMimirToJson(oper:mimir.algebra.Operator) : String = {
    mimirThread.runOnThread[String](new MimirRunnable[String](){
      def run() = {
        val results = new java.util.Vector[Row]()
         var cols : Seq[String] = null
         var colsIndexes : Seq[Int] = null
         
         MimirVizier.db.query(oper)( resIter => {
             cols = resIter.schema.map(f => f._1)
             colsIndexes = resIter.schema.zipWithIndex.map( _._2)
             while(resIter.hasNext())
               results.add(resIter.next)
         })
         val resCSV = results.toArray[Row](Array()).seq.map(row => {
           val truples = colsIndexes.map( (i) => {
             val pval = row(i) match {
               case NullPrimitive() => "0"
               case x => x.asString
             }
             s"${cols(i)}:{val:${pval},det:${row.isColDeterministic(i)}}" 
           })
           s"{${truples.mkString(",")}, rowdet:${row.isDeterministic()}, prov:${row.provenance.asString}}"
         })
         returnedValue = Some(s"[${resCSV.mkString(",")}]")
      }
    }) match {
      case None => ""
      case Some(ret) => ret
    }
  }
  
  def queryMimir(query:String): (Vector[String], Vector[CleaningJobDataRow]) = {
    val oper = MimirVizier.parseQuery(query)
    queryMimir(oper)   
  }
  
  def queryMimir(oper:mimir.algebra.Operator): (Vector[String], Vector[CleaningJobDataRow]) = {
    val queryResults = mimirThread.runOnThread[(Vector[String], Vector[CleaningJobDataRow])](new MimirRunnable[(Vector[String], Vector[CleaningJobDataRow])](){
      def run() = {
        val results = new java.util.Vector[Row]()
         var cols : Seq[String] = null
         var colsIndexes : Seq[Int] = null
         
         MimirVizier.db.query(oper)( resIter => {
             cols = resIter.schema.map(f => f._1)
             colsIndexes = resIter.schema.zipWithIndex.map( _._2)
             while(resIter.hasNext())
               results.add(resIter.next)
         })
         val resCSV = results.toArray[Row](Array()).seq.map(row => {
           val truples = colsIndexes.map( (i) => {
             CleaningJobDataCell(row(i).toString, row.isColDeterministic(i))  
           })
           CleaningJobDataRow(truples.toVector, row.isDeterministic(), row.provenance.asString)
         })
         returnedValue = Some((cols.toVector, resCSV.toVector))
      }
    })
    queryResults match {
      case None => ( Vector[String](),  Vector[CleaningJobDataRow]())
      case Some(ret:(Vector[String], Vector[CleaningJobDataRow])) => ret
    }
  }
  
  def feedbackSrc(deviceID:String, model:String, idx:Int, args:Seq[String], repairValue:String): Unit = {
    val none = mimirThread.runOnThread[Boolean](new MimirRunnable[Boolean](){
      def run() = {
        val theModel = MimirVizier.db.models.get(model)
        theModel match {
          case sourcedFeedbackModel:SourcedFeedbackT[Any] => {
            mimir.models.FeedbackSource.feedbackSource = FeedbackSourceIdentifier(deviceID, SharedServlet.getUserName(deviceID))
            doFeedback(model, idx, args, repairValue)
            mimir.models.FeedbackSource.feedbackSource = FeedbackSourceIdentifier()
          }
          case _ => doFeedback(model, idx, args, repairValue)  
        }
        returnedValue = None
      }
    })
    println("")
  }
  
  abstract class MimirRunnable[ReturnType] extends Runnable {
    protected var returnedValue : Option[ReturnType] = None;
    def getReturnedValue() : Option[ReturnType] = {
      returnedValue
    }
  }
  
  var running = true;
  
  class MimirThread extends Thread {
    val execQueue = new LinkedBlockingQueue[MimirRunnable[_]]()
    val returnQueue = new LinkedBlockingQueue[Any]()
    override def run() = {
      MimirVizier.main(Array("--db","mimir_community_clean.db","--X","INLINE-VG"/*,"GPROM-PROVENANCE"*/, "NO-VISTRAILS", "QUIET-LOG", "LOG"))
      while(running){
        try {
          val runnable = execQueue.take()
          runnable.run()
          returnQueue.put( runnable.getReturnedValue )
        } catch {
          case t: Throwable => println("MimirThread: take interrupted: " + t.toString())
        }
        
      }
    }
    def runOnThread[ReturnType](runnable:MimirRunnable[ReturnType]) : Option[ReturnType] = {
      synchronized {
        try{
          execQueue.put(runnable)
          returnQueue.take().asInstanceOf[Option[ReturnType]]
        } catch {
          case t: Throwable => {
            println("MimirThread.runOnThread: take interrupted...")
            None
          }
        }
      }
    }
  }
  
  val mimirThread = new MimirThread()
}